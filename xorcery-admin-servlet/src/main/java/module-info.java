open module xorcery.admin.servlet {

    requires xorcery.core;
    requires xorcery.health.registry;
    requires xorcery.metricregistry.hk2;

    requires jakarta.inject;
    requires java.management;
    requires jdk.management;

    requires org.glassfish.hk2.api;
    requires org.glassfish.hk2.runlevel;

    requires org.eclipse.jetty.servlet;
    requires com.codahale.metrics;
    requires com.codahale.metrics.health;
    requires com.codahale.metrics.json;
    requires com.codahale.metrics.jvm;
    requires io.dropwizard.metrics.servlets;

    requires org.apache.logging.log4j;
}