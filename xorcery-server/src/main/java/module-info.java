open module xorcery.server {
    exports com.exoreaction.xorcery.service.jetty.server;
    exports com.exoreaction.xorcery.service.jersey.server;
    exports com.exoreaction.xorcery.service.jersey.server.resources;

    requires transitive xorcery.configuration.api;
    requires transitive xorcery.metricregistry;
    requires xorcery.jsonapi;
    requires xorcery.service.api;
    requires xorcery.keystores;

    requires transitive jakarta.inject;
    requires transitive jakarta.validation;
    requires transitive jakarta.annotation;
    requires transitive jakarta.activation;
    requires transitive jakarta.ws.rs;
    requires transitive org.eclipse.jetty.server;
    requires jersey.server;

    requires org.glassfish.hk2.api;
    requires org.glassfish.hk2.runlevel;

    requires io.dropwizard.metrics.jetty11;

    requires org.eclipse.jetty.servlet;
    requires org.eclipse.jetty.alpn.java.server;
    requires org.eclipse.jetty.websocket.jetty.server;
    requires jersey.container.servlet.core;

    requires org.apache.logging.log4j;
    requires org.eclipse.jetty.http2.server;
}