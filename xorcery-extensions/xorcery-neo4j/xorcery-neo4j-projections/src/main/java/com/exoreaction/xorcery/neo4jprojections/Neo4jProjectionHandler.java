package com.exoreaction.xorcery.neo4jprojections;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.domainevents.api.DomainEventMetadata;
import com.exoreaction.xorcery.domainevents.api.MetadataEvents;
import com.exoreaction.xorcery.neo4j.client.GraphDatabase;
import com.exoreaction.xorcery.neo4jprojections.api.ProjectionStreamContext;
import com.exoreaction.xorcery.neo4jprojections.spi.Neo4jEventProjection;
import com.exoreaction.xorcery.opentelemetry.OpenTelemetryUnits;
import com.exoreaction.xorcery.reactivestreams.api.ContextViewElement;
import com.exoreaction.xorcery.reactivestreams.extras.operators.SmartBatchingOperator;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.ObservableLongGauge;
import io.opentelemetry.semconv.SchemaUrls;
import jakarta.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.api.IterableProvider;
import org.jvnet.hk2.annotations.Service;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.TransientTransactionFailureException;
import org.neo4j.memory.MemoryLimitExceededException;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SynchronousSink;
import reactor.core.scheduler.Schedulers;
import reactor.util.context.Context;
import reactor.util.context.ContextView;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import static com.exoreaction.xorcery.reactivestreams.api.ContextViewElement.missing;
import static io.opentelemetry.context.Context.taskWrapping;

@Service
public class Neo4jProjectionHandler
        implements BiFunction<Flux<MetadataEvents>, ContextView, Publisher<MetadataEvents>> {

    private final Neo4jProjectionsConfiguration projectionsConfiguration;

    private final GraphDatabaseService database;
    private final IterableProvider<Neo4jEventProjection> projectionProviders;
    private final Logger logger;

    private final LongHistogram batchSizeHistogram;
    private final DoubleHistogram writeLatencyHistogram;
    private final DoubleHistogram projectionLatencyHistogram;
    private final DoubleHistogram commitLatencyHistogram;
    private final Meter meter;

    @Inject
    public Neo4jProjectionHandler(
            GraphDatabase database,
            IterableProvider<Neo4jEventProjection> projectionProviders,
            Configuration configuration,
            OpenTelemetry openTelemetry,
            Logger logger
    ) {
        this.database = database.getGraphDatabaseService();
        this.projectionProviders = projectionProviders;
        this.logger = logger;
        this.projectionsConfiguration = new Neo4jProjectionsConfiguration(configuration.getConfiguration("neo4jprojections"));
        meter = openTelemetry.meterBuilder(getClass().getName())
                .setSchemaUrl(SchemaUrls.V1_25_0)
                .setInstrumentationVersion(getClass().getPackage().getImplementationVersion())
                .build();
        batchSizeHistogram = meter.histogramBuilder("neo4j.projection.batchsize")
                .ofLongs().setUnit("{count}").build();
        writeLatencyHistogram = meter.histogramBuilder("neo4j.projection.latency")
                .setUnit(OpenTelemetryUnits.SECONDS).build();
        projectionLatencyHistogram = meter.histogramBuilder("neo4j.projection.projectLatency")
                .setUnit(OpenTelemetryUnits.SECONDS).build();
        commitLatencyHistogram = meter.histogramBuilder("neo4j.projection.commitLatency")
                .setUnit(OpenTelemetryUnits.SECONDS).build();
    }

    @Override
    public Publisher<MetadataEvents> apply(Flux<MetadataEvents> metadataEventsFlux, ContextView contextView) {

        try {
            String projectionId = new ContextViewElement(contextView).getString(ProjectionStreamContext.projectionId)
                    .orElseThrow(missing(ProjectionStreamContext.projectionId));
            logger.info("Starting Neo4j projection with id " + projectionId);
            Optional<ProjectionModel> currentProjection = getCurrentProjection(projectionId);
            Attributes attributes = Attributes.of(AttributeKey.stringKey("neo4j.projection.projectionId"), projectionId);
            List<Neo4jEventProjection> projections = new ArrayList<>();
            projectionProviders.forEach(projections::add);
            Handler handler = new Handler(projections, projectionId, attributes, meter, currentProjection.flatMap(ProjectionModel::getProjectionPosition).orElse(-1L));
            ArrayBlockingQueue<MetadataEvents> smartBatchingQueue = new ArrayBlockingQueue<>(projectionsConfiguration.eventBatchSize());
            return Flux.from(currentProjection
                    .map(p -> SmartBatchingOperator.smartBatching(handler, () -> smartBatchingQueue, () -> taskWrapping(Schedulers.boundedElastic()::schedule))
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

                        return SmartBatchingOperator.smartBatching(handler, () -> smartBatchingQueue, () -> taskWrapping(Schedulers.boundedElastic()::schedule))
                                .apply(metadataEventsFlux, contextView);
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
            implements BiConsumer<Collection<MetadataEvents>, SynchronousSink<Collection<MetadataEvents>>>, AutoCloseable {

        private final Attributes attributes;
        private final ObservableLongGauge positionGauge;
        private final List<Neo4jEventProjection> projections;
        private final String projectionId;
        long position = -1;

        public Handler(List<Neo4jEventProjection> projections, String projectionId, Attributes attributes, Meter meter, long currentPosition) {
            this.projections = projections;
            this.projectionId = projectionId;
            this.position = currentPosition;
            this.attributes = attributes;
            this.positionGauge = meter.gaugeBuilder("neo4j.projection.position")
                    .ofLongs().setUnit("{count}").buildWithCallback(callback -> callback.record(position, attributes));
        }

        @Override
        public void accept(Collection<MetadataEvents> events, SynchronousSink<Collection<MetadataEvents>> sink) {
            final long start = System.nanoTime();
            final int eventsSize = events.size();
            int maxEvents = eventsSize;
            int committed = 0;
            Iterator<MetadataEvents> metadataEventsIterator = events.iterator();
            Transaction tx = database.beginTx();
            while (true) {
                try {
                    int eventCount = 0;
                    long startProjection = System.nanoTime();
                    while (metadataEventsIterator.hasNext()) {
                        MetadataEvents metadataEvents = metadataEventsIterator.next();

                        if (maxEvents == 1 && eventsSize > maxEvents)
                        {
                            logger.debug("Handling a single problematic event", metadataEvents);
                        }
                        metadataEvents.metadata().toBuilder().add(ProjectionStreamContext.projectionId, projectionId);
                        for (Neo4jEventProjection projection : projections) {
                            projection.write(metadataEvents, tx);
                        }

                        if (++eventCount == maxEvents || !metadataEventsIterator.hasNext()) {

                            double projectDuration = System.nanoTime() - startProjection;
                            projectionLatencyHistogram.record(projectDuration / 1_000_000_000.0, attributes);

                            position = ((Number) metadataEvents
                                    .metadata()
                                    .getLong(DomainEventMetadata.streamPosition)
                                    .orElse(position + eventsSize))
                                    .longValue();
                            long timestamp = ((Number) metadataEvents
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

                            long startCommit = System.nanoTime();
                            tx.commit();
                            tx.close();
                            double commitDuration = System.nanoTime() - startCommit;
                            commitLatencyHistogram.record(commitDuration / 1_000_000_000.0, attributes);

                            long stop = System.nanoTime();
                            batchSizeHistogram.record(eventCount, attributes);
                            double writeDuration = stop - start;
                            writeLatencyHistogram.record(writeDuration / 1_000_000_000.0, attributes);

                            if (logger.isTraceEnabled())
                                logger.trace("Committed " + eventsSize);

                            if (metadataEventsIterator.hasNext())
                            {
                                committed += eventCount;
                                if (maxEvents < eventsSize - committed)
                                {
                                    logger.info("Resized batch complete");
                                    maxEvents = eventsSize - committed;
                                }
                                tx = database.beginTx();
                            } else
                            {
                                tx = null;
                            }
                        }
                    }
                    if (tx != null)
                    {
                        tx.close();
                    }
                    break;
                } catch (MemoryLimitExceededException |TransientTransactionFailureException e) {
                    tx.rollback();
                    tx.close();

                    maxEvents /= 2;

                    if (maxEvents == 0)
                    {
                        logger.error("Transaction too large even with just one event, stopping projection");
                        sink.error(e);
                        return;
                    } else
                    {
                        logger.warn("Transaction too large, trying again with smaller batch size:"+maxEvents);
                        tx = database.beginTx();
                    }
                    metadataEventsIterator = events.iterator();
                    // Remove already processed items
                    for (int i = 0; i < committed; i++) {
                        metadataEventsIterator.next();
                    }
                } catch (Throwable e) {
                    sink.error(e);
                    return;
                }
            }
            sink.next(events);
        }

        @Override
        public void close() {
            logger.debug("Close projection " + projectionId);
            positionGauge.close();
        }
    }
}
