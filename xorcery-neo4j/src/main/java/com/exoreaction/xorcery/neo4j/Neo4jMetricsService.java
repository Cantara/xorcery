package com.exoreaction.xorcery.neo4j;

import com.exoreaction.xorcery.neo4j.client.GraphDatabase;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.semconv.SemanticAttributes;
import jakarta.inject.Inject;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;
import org.neo4j.kernel.impl.factory.GraphDatabaseFacade;
import org.neo4j.kernel.impl.transaction.stats.TransactionCounters;

@Service(name = "neo4j.metrics")
@RunLevel(4)
public class Neo4jMetricsService {

    @Inject
    public Neo4jMetricsService(GraphDatabase graphDatabase, OpenTelemetry openTelemetry) {

        Meter meter = openTelemetry.meterBuilder(getClass().getName())
                .setSchemaUrl(SemanticAttributes.SCHEMA_URL)
                .setInstrumentationVersion(getClass().getPackage().getImplementationVersion())
                .build();

        TransactionCounters txCounters = ((GraphDatabaseFacade) graphDatabase.getGraphDatabaseService()).getDependencyResolver().resolveDependency(TransactionCounters.class);
        meter.gaugeBuilder("neo4j.transaction.active")
                .setUnit("{transaction}").ofLongs().buildWithCallback(m->m.record(txCounters.getNumberOfActiveTransactions()));
        meter.gaugeBuilder("neo4j.transaction.committed")
                .setUnit("{transaction}").ofLongs().buildWithCallback(m->m.record(txCounters.getNumberOfCommittedTransactions()));
        meter.gaugeBuilder("neo4j.transaction.rolledBack")
                .setUnit("{transaction}").ofLongs().buildWithCallback(m->m.record(txCounters.getNumberOfRolledBackTransactions()));
        meter.gaugeBuilder("neo4j.transaction.peakConcurrent")
                .setUnit("{transaction}").ofLongs().buildWithCallback(m->m.record(txCounters.getPeakConcurrentNumberOfTransactions()));
    }
}
