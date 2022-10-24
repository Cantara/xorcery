open module xorcery.metricregistry {

    requires transitive com.codahale.metrics;

    requires com.codahale.metrics.jmx;
    requires org.glassfish.hk2.api;
    requires jakarta.inject;
}