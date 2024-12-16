package dev.xorcery.neo4jprojections;

import dev.xorcery.configuration.Configuration;
import dev.xorcery.domainevents.api.DomainEvent;
import dev.xorcery.domainevents.api.DomainEventMetadata;
import dev.xorcery.domainevents.api.JsonDomainEvent;
import dev.xorcery.domainevents.api.MetadataEvents;
import dev.xorcery.neo4j.client.GraphDatabase;
import dev.xorcery.neo4jprojections.api.ProjectionStreamContext;
import dev.xorcery.neo4jprojections.spi.Neo4jEventProjection;
import dev.xorcery.opentelemetry.OpenTelemetryUnits;
import dev.xorcery.reactivestreams.api.ContextViewElement;
import dev.xorcery.reactivestreams.extras.operators.SmartBatchingOperator;
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
import org.neo4j.kernel.impl.coreapi.InternalTransaction;
import org.neo4j.memory.MemoryLimitExceededException;
import org.neo4j.memory.MemoryTracker;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SynchronousSink;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.util.context.Context;
import reactor.util.context.ContextView;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import static dev.xorcery.collections.Element.missing;
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
    private final long highestSafeUsage;

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

        highestSafeUsage = projectionsConfiguration.getMaxTransactionSize()-projectionsConfiguration.getTransactionMemoryUsageMargin();
    }

    @Override
    public Publisher<MetadataEvents> apply(Flux<MetadataEvents> metadataEventsFlux, ContextView contextView) {

        try {
            String projectionId = new ContextViewElement(contextView).getString(ProjectionStreamContext.projectionId)
                    .orElseThrow(missing(ProjectionStreamContext.projectionId));
            logger.info("Starting Neo4j projection with id " + projectionId);
            Optional<ProjectionModel> currentProjection = getProjection(projectionId);
            Attributes attributes = Attributes.of(AttributeKey.stringKey("neo4j.projection.projectionId"), projectionId);
            List<Neo4jEventProjection> projections = new ArrayList<>();
            projectionProviders.forEach(projections::add);
            Handler handler = new Handler(projections, projectionId, attributes, meter, currentProjection.flatMap(ProjectionModel::getProjectionPosition).orElse(-1L));
            ArrayBlockingQueue<MetadataEvents> smartBatchingQueue = new ArrayBlockingQueue<>(projectionsConfiguration.eventBatchSize());
            int maxThreadCount = projectionsConfiguration.getMaxThreadCount();
            Scheduler scheduler = maxThreadCount == -1
                    ? Schedulers.boundedElastic()
                    :
                    maxThreadCount == 1
                            ? Schedulers.single()
                            : Schedulers.newBoundedElastic(maxThreadCount, 1024, "Neo4j Projection");
            return Flux.from(currentProjection
                    .map(p -> SmartBatchingOperator.smartBatching(handler, () -> smartBatchingQueue, () -> taskWrapping(scheduler::schedule))
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
                            createParameters.put(Projection.id.name(), projectionId);
                            Map<String, String> props = new HashMap<>();
                            contextView.forEach((k, v) ->
                            {
                                if (!k.toString().equals(ProjectionStreamContext.projectionId.name())) {
                                    props.put(k.toString(), v.toString());
                                }
                            });
                            createParameters.put("createdOn", System.currentTimeMillis());
                            createParameters.put("props", props);
                            tx.execute("""
                                    MERGE (projection:Projection {id:$id})
                                    ON CREATE SET
                                    projection.createdOn = $createdOn,
                                    projection += $props
                                    ON MATCH SET
                                    projection.projectionPosition = null,
                                    projection.createdOn = $createdOn,
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

    public Optional<ProjectionModel> getProjection(String projectionId) {
        return database.executeTransactionally("""
                MATCH (projection:Projection {id:$id})
                RETURN properties(projection) as properties
                """, Map.of(Projection.id.name(), projectionId), result ->
                result.hasNext() && result.next().get("properties") instanceof Map properties
                        ? Optional.of(new ProjectionModel(properties))
                        : Optional.empty());
    }

    public List<ProjectionModel> getProjections() {
        return database.executeTransactionally("""
                MATCH (projection:Projection)
                RETURN properties(projection) as properties
                """, Collections.emptyMap(), result -> {
                    List<ProjectionModel> projections = new ArrayList<>();
                    while (result.hasNext())
                    {
                        if (result.next().get("properties") instanceof Map properties)
                        {
                            projections.add(new ProjectionModel(properties));
                        }
                    }
                    return projections;
                });
    }

    private class Handler
            implements BiConsumer<Collection<MetadataEvents>, SynchronousSink<Collection<MetadataEvents>>>, AutoCloseable {

        private final Attributes attributes;
        private final ObservableLongGauge positionGauge;
        private final List<Neo4jEventProjection> projections;
        private final String projectionId;
        private final AtomicLong position = new AtomicLong(-1);

        public Handler(List<Neo4jEventProjection> projections, String projectionId, Attributes attributes, Meter meter, long currentPosition) {
            this.projections = projections;
            this.projectionId = projectionId;
            this.position.set(currentPosition);
            this.attributes = attributes;
            this.positionGauge = meter.gaugeBuilder("neo4j.projection.position")
                    .ofLongs().setUnit("{count}").buildWithCallback(callback -> callback.record(position.get(), attributes));
        }

        @Override
        public void accept(Collection<MetadataEvents> events, SynchronousSink<Collection<MetadataEvents>> sink) {
            final long start = System.nanoTime();
            final int eventsSize = events.size();
            int maxEvents = eventsSize;
            int committed = 0;
            Iterator<MetadataEvents> metadataEventsIterator = events.iterator();
            Transaction tx = database.beginTx();
            MemoryTracker memoryTracker = tx instanceof InternalTransaction it ? it.kernelTransaction().memoryTracker() : null;

            MetadataEvents metadataEvents = null;
            while (true) {
                try {
                    int eventCount = 0;
                    long startProjection = System.nanoTime();
                    while (metadataEventsIterator.hasNext()) {
                        metadataEvents = metadataEventsIterator.next();

                        if (maxEvents == 1 && eventsSize > maxEvents) {
                            logger.debug("Handling a single problematic event", metadataEvents);
                        }
                        metadataEvents.metadata().toBuilder().add(ProjectionStreamContext.projectionId, projectionId);
                        for (Neo4jEventProjection projection : projections) {
                            projection.write(metadataEvents, tx);
                        }

                        if (++eventCount == maxEvents || !metadataEventsIterator.hasNext() || (memoryTracker != null && memoryTracker.estimatedHeapMemory() > highestSafeUsage)) {

                            double projectDuration = System.nanoTime() - startProjection;
                            projectionLatencyHistogram.record(projectDuration / 1_000_000_000.0, attributes);

                            position.set(((Number) metadataEvents
                                    .metadata()
                                    .getLong(DomainEventMetadata.streamPosition)
                                    .orElse(position.get() + eventsSize))
                                    .longValue());
                            long timestamp = ((Number) metadataEvents
                                    .metadata()
                                    .getLong(DomainEventMetadata.timestamp)
                                    .orElseGet(System::currentTimeMillis))
                                    .longValue();
                            Map<String, Object> updateParameters = new HashMap<>();
                            updateParameters.put(Projection.id.name(), projectionId);
                            updateParameters.put(Projection.projectionPosition.name(), position.get());
                            updateParameters.put(Projection.projectionTimestamp.name(), timestamp);
                            tx.execute("""
                                    MATCH (projection:Projection {id:$id}) SET 
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

                            if (metadataEventsIterator.hasNext()) {
                                committed += eventCount;
                                if (maxEvents < eventsSize - committed) {
                                    logger.info("Resized batch complete");
                                    maxEvents = eventsSize - committed;
                                }
                                tx = database.beginTx();
                            } else {
                                tx = null;
                            }
                        }
                    }
                    if (tx != null) {
                        tx.close();
                    }
                    break;
                } catch (MemoryLimitExceededException | TransientTransactionFailureException e) {
                    tx.rollback();
                    tx.close();

                    maxEvents /= 2;

                    // Gather debug info
                    {
                        metadataEventsIterator = events.iterator();
                        // Remove already processed items
                        for (int i = 0; i < committed; i++) {
                            metadataEventsIterator.next();
                        }
                        // Collect remaining command and event names
                        Set<String> commandNames = new HashSet<>();
                        Set<String> eventNames = new HashSet<>();
                        int eventCount = 0;
                        while (metadataEventsIterator.hasNext())
                        {
                            MetadataEvents event = metadataEventsIterator.next();
                            event.metadata().getString("commandName").ifPresent(commandNames::add);
                            for (DomainEvent domainEvent : event.data()) {
                                if (domainEvent instanceof JsonDomainEvent jsonDomainEvent)
                                {
                                    eventNames.add(jsonDomainEvent.getName());
                                    eventCount++;
                                }
                            }
                        }

                        if (maxEvents == 0) {
                            logger.error("Transaction too large even with just one event, stopping projection");
                            sink.error(e);
                            return;
                        } else {
                            logger.warn("Transaction too large, trying again with smaller batch size:{}, position:{}, commands:{}, events:{}, event count:{}",maxEvents, position.get(), commandNames, eventNames, eventCount);
                            tx = database.beginTx();
                        }
                    }
                    metadataEventsIterator = events.iterator();
                    // Remove already processed items
                    for (int i = 0; i < committed; i++) {
                        metadataEventsIterator.next();
                    }
                } catch (Throwable e) {
                    long position = -1;
                    if (metadataEvents != null) {
                        position = ((Number) metadataEvents
                                .metadata()
                                .getLong(DomainEventMetadata.streamPosition)
                                .orElse(position + eventsSize))
                                .longValue();
                    }

                    sink.error(new IllegalArgumentException("Could not handle event at stream position: " + position, e));
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
