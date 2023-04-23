open module xorcery.admin.servlet {
    requires org.glassfish.hk2.api;
    requires jakarta.inject;
    requires org.eclipse.jetty.servlet;
    requires com.codahale.metrics;
    requires io.dropwizard.metrics.servlets;
    requires com.codahale.metrics.health;
    requires xorcery.health.registry;
    requires com.codahale.metrics.jvm;
    requires java.management;
    requires jdk.management;
    requires org.apache.logging.log4j;
    requires org.glassfish.hk2.runlevel;
    requires xorcery.metricregistry.hk2;
    requires com.codahale.metrics.json;
}