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
package com.exoreaction.xorcery.neo4j;

import com.exoreaction.xorcery.lang.AutoCloseables;
import com.exoreaction.xorcery.neo4j.client.GraphDatabase;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.semconv.SemanticAttributes;
import jakarta.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;
import org.neo4j.kernel.impl.factory.GraphDatabaseFacade;
import org.neo4j.kernel.impl.transaction.stats.TransactionCounters;

@Service(name = "neo4j.metrics")
@RunLevel(4)
public class Neo4jMetricsService
    implements PreDestroy
{
    private final Logger logger;
    private AutoCloseables closeables = new AutoCloseables();

    @Inject
    public Neo4jMetricsService(GraphDatabase graphDatabase, OpenTelemetry openTelemetry, Logger logger) {
        this.logger = logger;

        Meter meter = openTelemetry.meterBuilder(getClass().getName())
                .setSchemaUrl(SemanticAttributes.SCHEMA_URL)
                .setInstrumentationVersion(getClass().getPackage().getImplementationVersion())
                .build();

        TransactionCounters txCounters = ((GraphDatabaseFacade) graphDatabase.getGraphDatabaseService()).getDependencyResolver().resolveDependency(TransactionCounters.class);
        closeables.add(meter.gaugeBuilder("neo4j.transaction.active")
                .setUnit("{transaction}").ofLongs().buildWithCallback(m->m.record(txCounters.getNumberOfActiveTransactions())));
        closeables.add(meter.gaugeBuilder("neo4j.transaction.committed")
                .setUnit("{transaction}").ofLongs().buildWithCallback(m->m.record(txCounters.getNumberOfCommittedTransactions())));
        closeables.add(meter.gaugeBuilder("neo4j.transaction.rolledBack")
                .setUnit("{transaction}").ofLongs().buildWithCallback(m->m.record(txCounters.getNumberOfRolledBackTransactions())));
        closeables.add(meter.gaugeBuilder("neo4j.transaction.peakConcurrent")
                .setUnit("{transaction}").ofLongs().buildWithCallback(m->m.record(txCounters.getPeakConcurrentNumberOfTransactions())));
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
