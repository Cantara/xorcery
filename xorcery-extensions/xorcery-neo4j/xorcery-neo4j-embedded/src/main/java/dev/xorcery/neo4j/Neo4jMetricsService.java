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
package dev.xorcery.neo4j;

import dev.xorcery.configuration.Configuration;
import dev.xorcery.lang.AutoCloseables;
import dev.xorcery.neo4j.client.GraphDatabase;
import dev.xorcery.opentelemetry.OpenTelemetryUnits;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.semconv.SchemaUrls;
import jakarta.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;
import org.neo4j.io.pagecache.monitoring.PageCacheCounters;
import org.neo4j.kernel.impl.factory.GraphDatabaseFacade;
import org.neo4j.kernel.impl.transaction.stats.CheckpointCounters;
import org.neo4j.kernel.impl.transaction.stats.TransactionCounters;

import java.time.Duration;
import java.util.function.Function;

@Service(name = "neo4j.metrics")
@RunLevel(4)
public class Neo4jMetricsService
    implements PreDestroy
{
    private final Logger logger;
    private final AutoCloseables closeables = new AutoCloseables();

    @Inject
    public Neo4jMetricsService(GraphDatabase graphDatabase, OpenTelemetry openTelemetry, Configuration configuration, Logger logger) {
        this.logger = logger;

        Meter meter = openTelemetry.meterBuilder(getClass().getName())
                .setSchemaUrl(SchemaUrls.V1_25_0)
                .setInstrumentationVersion(getClass().getPackage().getImplementationVersion())
                .build();

        TransactionCounters txCounters = ((GraphDatabaseFacade) graphDatabase.getGraphDatabaseService()).getDependencyResolver().resolveDependency(TransactionCounters.class);
        CheckpointCounters checkpointCounters = ((GraphDatabaseFacade) graphDatabase.getGraphDatabaseService()).getDependencyResolver().resolveDependency(CheckpointCounters.class);
        PageCacheCounters pageCacheCounters = ((GraphDatabaseFacade) graphDatabase.getGraphDatabaseService()).getDependencyResolver().resolveDependency(PageCacheCounters.class);

        configuration.getObjectAs("neo4jdatabase.metrics.attributes", Function.identity()).ifPresent(attributes ->
        {
            attributes.properties().forEach(entry ->
            {
                if (entry.getValue().isNull())
                    return;
                switch (entry.getValue().textValue())
                {
                    // Transactions
                    case "neo4j.transaction.active"->
                            closeables.add(meter.gaugeBuilder(entry.getKey())
                                    .setUnit("{transaction}").ofLongs().buildWithCallback(m->m.record(txCounters.getNumberOfActiveTransactions())));
                    case "neo4j.transaction.committed"->
                            closeables.add(meter.gaugeBuilder(entry.getKey())
                                    .setUnit("{transaction}").ofLongs().buildWithCallback(m->m.record(txCounters.getNumberOfCommittedTransactions())));
                    case "neo4j.transaction.rolledBack"->
                            closeables.add(meter.gaugeBuilder(entry.getKey())
                                    .setUnit("{transaction}").ofLongs().buildWithCallback(m->m.record(txCounters.getNumberOfRolledBackTransactions())));
                    case "neo4j.transaction.peakConcurrent"->
                            closeables.add(meter.gaugeBuilder(entry.getKey())
                                    .setUnit("{transaction}").ofLongs().buildWithCallback(m->m.record(txCounters.getPeakConcurrentNumberOfTransactions())));

                    // Checkpoints
                    case "neo4j.checkpoint.count"->
                            closeables.add(meter.gaugeBuilder(entry.getKey())
                                    .setUnit("{checkpoint}").ofLongs().buildWithCallback(m->m.record(checkpointCounters.numberOfCheckPoints())));
                    case "neo4j.checkpoint.flushed"->
                            closeables.add(meter.gaugeBuilder(entry.getKey())
                                    .setUnit(OpenTelemetryUnits.BYTES).ofLongs().buildWithCallback(m->m.record(checkpointCounters.flushedBytes())));
                    case "neo4j.checkpoint.totalTime"->
                            closeables.add(meter.gaugeBuilder(entry.getKey())
                                    .setUnit(OpenTelemetryUnits.SECONDS).ofLongs().buildWithCallback(m->m.record(Duration.ofMillis(checkpointCounters.checkPointAccumulatedTotalTimeMillis()).toSeconds())));

                    // Page cache
                    case "neo4j.pagecache.hits"->
                            closeables.add(meter.gaugeBuilder(entry.getKey())
                                    .setUnit("{hit}").ofLongs().buildWithCallback(m->m.record(pageCacheCounters.hits())));
                    case "neo4j.pagecache.faults"->
                            closeables.add(meter.gaugeBuilder(entry.getKey())
                                    .setUnit("{fault}").ofLongs().buildWithCallback(m->m.record(pageCacheCounters.faults())));
                    case "neo4j.pagecache.hitRatio"->
                            closeables.add(meter.gaugeBuilder(entry.getKey())
                                    .setUnit(OpenTelemetryUnits.UNIT).buildWithCallback(m->m.record(pageCacheCounters.hitRatio())));
                    case "neo4j.pagecache.flushes"->
                            closeables.add(meter.gaugeBuilder(entry.getKey())
                                    .setUnit("{flush}").ofLongs().buildWithCallback(m->m.record(pageCacheCounters.flushes())));


                }
            });
        });
    }

    @Override
    public void preDestroy() {
        try {
            closeables.close();
        } catch (Exception e) {
            logger.warn("Could not close metrics", e);
        }
    }
}
