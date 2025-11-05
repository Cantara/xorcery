/*
 * Copyright © 2022 eXOReaction AS (rickard@exoreaction.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.xorcery.neo4jprojections;

import dev.xorcery.configuration.Configuration;
import dev.xorcery.domainevents.api.DomainEvent;
import dev.xorcery.domainevents.api.DomainEventMetadata;
import dev.xorcery.domainevents.api.JsonDomainEvent;
import dev.xorcery.domainevents.api.MetadataEvents;
import dev.xorcery.neo4j.TransactionContext;
import dev.xorcery.neo4j.client.GraphDatabase;
import dev.xorcery.neo4jprojections.api.ProjectionStreamContext;
import dev.xorcery.neo4jprojections.spi.Neo4jEventProjection;
import dev.xorcery.neo4jprojections.spi.SignalProjectionIsolationException;
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
import static dev.xorcery.lang.Exceptions.unwrap;
import static io.opentelemetry.context.Context.taskWrapping;
import static reactor.core.scheduler.Schedulers.DEFAULT_BOUNDED_ELASTIC_QUEUESIZE;

@Service
public class Neo4jProjectionHandler
        implements BiFunction<Flux<MetadataEvents>, ContextView, Publisher<MetadataEvents>> {

    private static final String ISOLATED_PROJECTION_MODE = "isolatedProjectionMode";

    /**
     * Event handlers may call this at beginning of their code to ensure that there are no other events in the same transaction.
     * On first call an exception will be thrown, triggering the commit of existing events and starting a new transaction.
     * On second call it will continue, as the transaction is marked as nonBatchMode
     *
     * @param transaction
     * @throws SignalProjectionIsolationException
     */
    public static void ensureIsolatedProjection(Transaction transaction) {
        if (!TransactionContext.<Boolean>getTransactionContext(transaction, ISOLATED_PROJECTION_MODE).orElse(false)) {
            throw new SignalProjectionIsolationException();
        }
    }

    private final Neo4jProjectionsConfiguration projectionsConfiguration;

    private final GraphDatabaseService database;
    private final IterableProvider<Neo4jEventProjection> projectionProviders;
    private final Logger logger;

    private final LongHistogram batchSizeHistogram;
    private final DoubleHistogram writeLatencyHistogram;
    private final DoubleHistogram projectionLatencyHistogram;
    private final DoubleHistogram commitLatencyHistogram;
    private final LongHistogram txMemoryHistogram;
    private final Meter meter;
    private final long highestSafeUsage;
    private final Scheduler scheduler;

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
        txMemoryHistogram = meter.histogramBuilder("neo4j.projection.transactionMemoryUsage")
                .ofLongs().setUnit(OpenTelemetryUnits.BYTES).build();

        highestSafeUsage = projectionsConfiguration.getMaxTransactionSize() - projectionsConfiguration.getTransactionMemoryUsageMargin();

        int maxThreadCount = projectionsConfiguration.getMaxThreadCount();
        scheduler = maxThreadCount == -1
                ? Schedulers.boundedElastic()
                : maxThreadCount == 1
                ? Schedulers.single()
                : Schedulers.newBoundedElastic(maxThreadCount, DEFAULT_BOUNDED_ELASTIC_QUEUESIZE, "Neo4j Projection");
    }

    @Override
    public Publisher<MetadataEvents> apply(Flux<MetadataEvents> metadataEventsFlux, ContextView contextView) {

        try {
            String projectionId = new ContextViewElement(contextView).getString(ProjectionStreamContext.projectionId)
                    .orElseThrow(missing(ProjectionStreamContext.projectionId));
            logger.debug("Starting Neo4j projection with id " + projectionId);
            Optional<ProjectionModel> currentProjection = getProjection(projectionId);
            Attributes attributes = Attributes.of(AttributeKey.stringKey("neo4j.projection.projectionId"), projectionId);
            List<Neo4jEventProjection> projections = new ArrayList<>();
            projectionProviders.forEach(projections::add);
            Handler handler = new Handler(projections, projectionId, attributes, meter, currentProjection.flatMap(ProjectionModel::getProjectionPosition).orElse(-1L));
            ArrayBlockingQueue<MetadataEvents> smartBatchingQueue = new ArrayBlockingQueue<>(projectionsConfiguration.eventBatchSize());
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

                        return SmartBatchingOperator.smartBatching(handler, () -> smartBatchingQueue, () -> taskWrapping(scheduler::schedule))
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
            while (result.hasNext()) {
                if (result.next().get("properties") instanceof Map properties) {
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
            try {
                final long start = System.nanoTime();
                final int eventsSize = events.size();
                int maxEvents = eventsSize;
                int committed = 0;
                Iterator<MetadataEvents> metadataEventsIterator = events.iterator();
                Transaction tx = database.beginTx();

                MetadataEvents metadataEvents = null;
                boolean beforeIsolatedProjectionMode = false;
                boolean isolatedProjectionMode = false;
                while (true) {
                    int eventCount = 0;
                    MemoryTracker memoryTracker = tx instanceof InternalTransaction it ? it.kernelTransaction().memoryTracker() : null;
                    try {
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

                                long newPosition = ((Number) metadataEvents
                                        .metadata()
                                        .getLong(DomainEventMetadata.streamPosition)
                                        .orElse(position.get() + eventCount))
                                        .longValue();
                                long timestamp = ((Number) metadataEvents
                                        .metadata()
                                        .getLong(DomainEventMetadata.timestamp)
                                        .orElseGet(System::currentTimeMillis))
                                        .longValue();
                                Map<String, Object> updateParameters = new HashMap<>();
                                updateParameters.put(Projection.id.name(), projectionId);
                                updateParameters.put(Projection.projectionPosition.name(), newPosition);
                                updateParameters.put(Projection.projectionTimestamp.name(), timestamp);
                                tx.execute("""
                                        MATCH (projection:Projection {id:$id}) SET 
                                        projection.projectionPosition=$projectionPosition,
                                        projection.projectionTimestamp=$projectionTimestamp
                                        RETURN projection
                                        """, updateParameters).close();

                                long startCommit = System.nanoTime();
                                long usedMemory = memoryTracker != null ? memoryTracker.estimatedHeapMemory() : -1;
                                tx.commit();
                                double commitDuration = System.nanoTime() - startCommit;
                                commitLatencyHistogram.record(commitDuration / 1_000_000_000.0, attributes);

                                long stop = System.nanoTime();
                                batchSizeHistogram.record(eventCount, attributes);
                                double writeDuration = stop - start;
                                writeLatencyHistogram.record(writeDuration / 1_000_000_000.0, attributes);

                                if (usedMemory != -1L) {
                                    txMemoryHistogram.record(usedMemory, attributes);
                                }

                                if (logger.isTraceEnabled())
                                    logger.trace("Committed " + eventsSize);

                                position.set(newPosition);

                                if (metadataEventsIterator.hasNext()) {
                                    committed += eventCount;
                                    tx = database.beginTx();
                                    eventCount = 0;

                                    if (beforeIsolatedProjectionMode){
                                        isolatedProjectionMode = true;
                                        TransactionContext.setTransactionContext(tx, ISOLATED_PROJECTION_MODE, true);
                                        maxEvents = 1;
                                        beforeIsolatedProjectionMode = false;
                                    } else if (isolatedProjectionMode){
                                        maxEvents = eventsSize;
                                        isolatedProjectionMode = false;
                                    }
                                } else {
                                    tx = null;
                                }
                            }
                        }
                        if (tx != null) {
                            tx.commit();
                        }
                        break;
                    } catch (MemoryLimitExceededException e) {
                        tx.rollback();

                        maxEvents /= 16;

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
                            int actualEventCount = 0;
                            while (metadataEventsIterator.hasNext()) {
                                MetadataEvents event = metadataEventsIterator.next();
                                event.metadata().getString("commandName").ifPresent(commandNames::add);
                                for (DomainEvent domainEvent : event.data()) {
                                    if (domainEvent instanceof JsonDomainEvent jsonDomainEvent) {
                                        eventNames.add(jsonDomainEvent.getName());
                                        actualEventCount++;
                                    }
                                }
                            }

                            if (maxEvents == 0) {
                                logger.error("Transaction too large even with just one event, stopping projection");
                                sink.error(e);
                                return;
                            } else {
                                logger.warn("Transaction too large, trying again with smaller batch size:{}, position:{}, commands:{}, events:{}, event count:{}", maxEvents, position.get(), commandNames, eventNames, actualEventCount);
                                tx = database.beginTx();
                            }
                        }

                        metadataEventsIterator = events.iterator();
                        // Remove already processed items
                        for (int i = 0; i < committed; i++) {
                            metadataEventsIterator.next();
                        }
                    } catch (TransientTransactionFailureException e) {
                        tx.rollback();

                        // Retry entire batch
                        logger.warn("Transient transaction failure, retrying entire batch for projection " + projectionId, e);
                        tx = database.beginTx();
                        metadataEventsIterator = events.iterator();
                        // Remove already processed items
                        for (int i = 0; i < committed; i++) {
                            metadataEventsIterator.next();
                        }
                    } catch (Throwable e) {
                        tx.rollback();
                        if (unwrap(e) instanceof SignalProjectionIsolationException){
                            tx = database.beginTx();
                            if (eventCount > 0){
                                beforeIsolatedProjectionMode = true;
                                maxEvents = eventCount; // Replay events up until the non-batched event
                            } else {
                                isolatedProjectionMode = true;
                                TransactionContext.setTransactionContext(tx, ISOLATED_PROJECTION_MODE, true);
                                maxEvents = 1;
                            }
                            metadataEventsIterator = events.iterator();
                            // Remove already processed items
                            for (int i = 0; i < committed; i++) {
                                metadataEventsIterator.next();
                            }
                        } else {
                            long position = -1;
                            if (metadataEvents != null) {
                                position = ((Number) metadataEvents
                                        .metadata()
                                        .getLong(DomainEventMetadata.streamPosition)
                                        .orElse(position + eventsSize))
                                        .longValue();
                            }

                            sink.error(new IllegalArgumentException("Could not handle event for projection " + projectionId + " at stream position: " + position, e));
                            return;
                        }
                    }
                }
                sink.next(events);
            } catch (Throwable e) {
                sink.error(new IllegalArgumentException("Could not handle event for projection " + projectionId + " at stream position: " + position, e));
            }
        }

        @Override
        public void close() {
            logger.debug("Close projection " + projectionId);
            positionGauge.close();
        }
    }
}
