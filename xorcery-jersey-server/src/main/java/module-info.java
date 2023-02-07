open module xorcery.jersey.server {
    exports com.exoreaction.xorcery.service.jersey.server;
    exports com.exoreaction.xorcery.service.jersey.server.resources;

    requires transitive xorcery.configuration.api;
    requires transitive xorcery.metricregistry;
    requires xorcery.jsonapi;
    requires xorcery.service.api;

    requires jersey.server;
    requires jersey.container.servlet.core;
    requires org.eclipse.jetty.servlet;

    requires jakarta.inject;
    requires org.glassfish.hk2.api;
    requires org.glassfish.hk2.runlevel;

    requires org.apache.logging.log4j;
}