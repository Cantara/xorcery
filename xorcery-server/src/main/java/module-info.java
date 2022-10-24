open module xorcery.server {
    exports com.exoreaction.xorcery.service.jetty.server;
    exports com.exoreaction.xorcery.service.jersey.server;
    exports com.exoreaction.xorcery.service.jersey.server.resources;

    requires transitive xorcery.core;
    requires xorcery.jsonapi;
    requires xorcery.metricregistry;

    requires transitive jakarta.inject;
    requires transitive jakarta.validation;
    requires transitive jakarta.annotation;
    requires transitive jakarta.activation;

    requires org.glassfish.hk2.api;
    requires jersey.hk2;
    requires jersey.server;

    requires io.dropwizard.metrics.jetty11;

    requires org.eclipse.jetty.server;
    requires org.eclipse.jetty.servlet;
    requires org.eclipse.jetty.websocket.jetty.server;
    requires jersey.container.servlet.core;

    requires org.apache.logging.log4j;
    requires org.eclipse.jetty.http2.server;
}