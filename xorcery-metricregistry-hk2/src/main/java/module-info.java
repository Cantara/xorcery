open module xorcery.metricregistry.hk2 {

    exports com.exoreaction.xorcery.service.metricregistry;

    requires com.codahale.metrics;
    requires com.codahale.metrics.jmx;
    requires org.glassfish.hk2.api;
    requires jakarta.inject;
}