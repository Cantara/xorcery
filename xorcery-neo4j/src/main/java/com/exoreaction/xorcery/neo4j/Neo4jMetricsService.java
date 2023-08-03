package com.exoreaction.xorcery.neo4j;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.exoreaction.xorcery.neo4j.client.GraphDatabase;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.kernel.impl.factory.GraphDatabaseFacade;
import org.neo4j.kernel.impl.transaction.stats.TransactionCounters;

@Service(name = "neo4j.metrics")
@RunLevel(4)
public class Neo4jMetricsService {

    @Inject
    public Neo4jMetricsService(GraphDatabase graphDatabase, Provider<MetricRegistry> metricRegistryProvider) {
        MetricRegistry metricRegistry = metricRegistryProvider.get();
        if (metricRegistry != null)
        {
            TransactionCounters txCounters = ((GraphDatabaseFacade) graphDatabase.getGraphDatabaseService()).getDependencyResolver().resolveDependency(TransactionCounters.class);
            metricRegistry.gauge("activeTransactions", (MetricRegistry.MetricSupplier<Gauge<Long>>) () -> txCounters::getNumberOfActiveTransactions);
            metricRegistry.gauge("committedTransactions", (MetricRegistry.MetricSupplier<Gauge<Long>>) () -> txCounters::getNumberOfCommittedTransactions);
            metricRegistry.gauge("rolledBackTransactions", (MetricRegistry.MetricSupplier<Gauge<Long>>) () -> txCounters::getNumberOfRolledBackTransactions);
            metricRegistry.gauge("peakConcurrentTransactions", (MetricRegistry.MetricSupplier<Gauge<Long>>) () -> txCounters::getPeakConcurrentNumberOfTransactions);
        }
    }
}
