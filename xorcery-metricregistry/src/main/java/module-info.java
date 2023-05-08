open module xorcery.metricregistry {

    exports com.exoreaction.xorcery.service.metricregistry;

    requires com.codahale.metrics;
    requires com.codahale.metrics.jmx;
    requires com.codahale.metrics.jvm;
    requires org.glassfish.hk2.api;
    requires jakarta.inject;
    requires org.glassfish.hk2.runlevel;
    requires java.management;
    requires jdk.management;
}