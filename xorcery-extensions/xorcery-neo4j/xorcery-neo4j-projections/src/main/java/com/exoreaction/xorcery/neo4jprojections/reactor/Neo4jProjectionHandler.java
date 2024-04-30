package com.exoreaction.xorcery.neo4jprojections.reactor;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.domainevents.api.DomainEventMetadata;
import com.exoreaction.xorcery.domainevents.api.MetadataEvents;
import com.exoreaction.xorcery.neo4j.client.GraphDatabase;
import com.exoreaction.xorcery.neo4jprojections.Projection;
import com.exoreaction.xorcery.neo4jprojections.ProjectionModel;
import com.exoreaction.xorcery.neo4jprojections.spi.Neo4jEventProjection;
import com.exoreaction.xorcery.reactivestreams.api.ContextViewElement;
import com.exoreaction.xorcery.reactivestreams.disruptor.DisruptorConfiguration;
import com.exoreaction.xorcery.reactivestreams.disruptor.SmartBatching;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.ObservableLongGauge;
import io.opentelemetry.semconv.SemanticAttributes;
import jakarta.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.api.IterableProvider;
import org.jvnet.hk2.annotations.Service;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SynchronousSink;
import reactor.util.context.Context;
import reactor.util.context.ContextView;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import static com.exoreaction.xorcery.reactivestreams.api.ContextViewElement.missing;

@Service
public class Neo4jProjectionHandler
        implements BiFunction<Flux<MetadataEvents>, ContextView, Publisher<MetadataEvents>> {

    private final GraphDatabaseService database;
    private final Logger logger;
    private final List<Neo4jEventProjection> projections = new ArrayList<>();

    private final DisruptorConfiguration configuration;
    private final LongHistogram batchSizeHistogram;
    private final DoubleHistogram writeLatencyHistogram;
    private final Meter meter;

    @Inject
    public Neo4jProjectionHandler(
            GraphDatabase database,
            IterableProvider<Neo4jEventProjection> projections,
            Configuration configuration,
            OpenTelemetry openTelemetry,
            Logger logger
    ) {
        this.database = database.getGraphDatabaseService();
        this.logger = logger;
        projections.forEach(this.projections::add);
        this.configuration = new DisruptorConfiguration(configuration.getConfiguration("neo4jprojections.disruptor"));
        meter = openTelemetry.meterBuilder(getClass().getName())
                .setSchemaUrl(SemanticAttributes.SCHEMA_URL)
                .setInstrumentationVersion(getClass().getPackage().getImplementationVersion())
                .build();
        batchSizeHistogram = meter.histogramBuilder("neo4j.projection.batchsize")
                .ofLongs().setUnit("{count}").build();
        writeLatencyHistogram = meter.histogramBuilder("neo4j.projection.latency")
                .setUnit("s").build();
    }

    @Override
    public Publisher<MetadataEvents> apply(Flux<MetadataEvents> metadataEventsFlux, ContextView contextView) {

        try {
            String projectionId = new ContextViewElement(contextView).getString(ProjectionStreamContext.projectionId)
                    .orElseThrow(missing(ProjectionStreamContext.projectionId));
            logger.info("Starting Neo4j projection with id " + projectionId);
            Optional<ProjectionModel> currentProjection = getCurrentProjection(projectionId);
            Attributes attributes = Attributes.of(AttributeKey.stringKey("neo4j.projection.projectionId"), projectionId);
            Handler handler = new Handler(projectionId, attributes, meter, currentProjection.flatMap(ProjectionModel::getProjectionPosition).orElse(-1L));
            return Flux.from(currentProjection
                    .map(p -> new SmartBatching<>(this.configuration, handler)
                            .apply(p.getProjectionPosition()
                                            .map(pos ->
                                            {
                                                logger.debug("Resuming from position:{}", pos);
                                                return metadataEventsFlux.contextWrite(Context.of(ProjectionStreamContext.projectionPosition, pos));
                                            })
                                            .orElse(metadataEventsFlux),
                                    contextView))
                    .orElseGet(() ->
                    {
                        logger.info("Creating projection {}", projectionId);

                        // Create projection in Neo4j
                        try (Transaction tx = database.beginTx()) {
                            Map<String, Object> createParameters = new HashMap<>();
                            createParameters.put(Projection.projectionId.name(), projectionId);
                            Map<String, String> props = new HashMap<>();
                            contextView.forEach((k, v) ->
                            {
                                if (!k.toString().equals(ProjectionStreamContext.projectionId.name())) {
                                    props.put(k.toString(), v.toString());
                                }
                            });
                            createParameters.put("props", props);
                            tx.execute("""
                                    MERGE (projection:Projection {id:$projectionId})
                                    ON CREATE SET
                                    projection += $props
                                    ON MATCH SET
                                    projection.projectionPosition = coalesce(projection.revision,null),
                                    projection += $props
                                    RETURN projection
                                    """, createParameters).close();
                            tx.commit();
                        }

                        return new SmartBatching<>(this.configuration, handler).apply(metadataEventsFlux, contextView);
                    })).doAfterTerminate(handler::close);
        } catch (RuntimeException e) {
            return Flux.error(e);
        }
    }

    public Optional<ProjectionModel> getCurrentProjection(String projectionId) {
        // Check if we already have written data for this projection before
        return database.executeTransactionally("""
                MATCH (projection:Projection {id:$projectionId})
                RETURN coalesce(projection.projectionPosition, projection.revision) as projectionPosition, projection.id as projectionId, projection.streamId as streamId
                """, Map.of(Projection.projectionId.name(), projectionId), result ->
                result.hasNext()
                        ? Optional.of(new ProjectionModel(result.next()))
                        : Optional.empty());
    }

    private class Handler
            implements BiConsumer<List<MetadataEvents>, SynchronousSink<List<MetadataEvents>>>, AutoCloseable {

        private final Attributes attributes;
        private final ObservableLongGauge positionGauge;
        private final String projectionId;
        long position = -1;

        public Handler(String projectionId, Attributes attributes, Meter meter, long currentPosition) {
            this.projectionId = projectionId;
            this.position = currentPosition;
            this.attributes = attributes;
            this.positionGauge = meter.gaugeBuilder("neo4j.projection.position")
                    .ofLongs().setUnit("{count}").buildWithCallback(callback -> callback.record(position, attributes));
        }

        @Override
        public void accept(List<MetadataEvents> events, SynchronousSink<List<MetadataEvents>> sink) {
            long start = System.nanoTime();
            try (Transaction tx = database.beginTx()) {
                for (MetadataEvents metadataEvents : events) {
                    metadataEvents.metadata().toBuilder().add(ProjectionStreamContext.projectionId, projectionId);
                    for (Neo4jEventProjection projection : projections) {
                        projection.write(metadataEvents, tx);
                    }
                }
                if (!events.isEmpty()) {
                    position = ((Number) events
                            .get(events.size() - 1)
                            .metadata()
                            .getLong(DomainEventMetadata.streamPosition)
                            .orElse(position + events.size()))
                            .longValue();
                    long timestamp = ((Number) events
                            .get(events.size() - 1)
                            .metadata()
                            .getLong(DomainEventMetadata.timestamp)
                            .orElseGet(System::currentTimeMillis))
                            .longValue();
                    Map<String, Object> updateParameters = new HashMap<>();
                    updateParameters.put(Projection.projectionId.name(), projectionId);
                    updateParameters.put(Projection.projectionPosition.name(), position);
                    updateParameters.put(Projection.projectionTimestamp.name(), timestamp);
                    tx.execute("""
                            MATCH (projection:Projection {id:$projectionId}) SET 
                            projection.projectionPosition=$projectionPosition,
                            projection.projectionTimestamp=$projectionTimestamp
                            RETURN projection
                            """, updateParameters).close();

                    tx.commit();
                    long stop = System.nanoTime();
                    batchSizeHistogram.record(events.size(),attributes);
                    double writeDuration = stop - start;
                    writeLatencyHistogram.record(writeDuration /  1_000_000_000.0, attributes);

                    if (logger.isTraceEnabled())
                        logger.trace("Committed " + events.size());
                }

            } catch (Throwable e) {
                sink.error(e);
                return;
            }
            sink.next(events);
        }

        @Override
        public void close() {
            logger.debug("Close projection "+projectionId);
            positionGauge.close();
        }
    }
}
