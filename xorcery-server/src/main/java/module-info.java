open module xorcery.server {
    exports com.exoreaction.xorcery.service.jetty.server;
    exports com.exoreaction.xorcery.service.jersey.server;
    exports com.exoreaction.xorcery.service.jersey.server.resources;
    exports com.exoreaction.xorcery.service.jetty.server.security;
    exports com.exoreaction.xorcery.service.jetty.server.security.jwt;
    exports com.exoreaction.xorcery.service.jetty.server.security.clientcert;

    requires transitive xorcery.configuration.api;
    requires transitive xorcery.metricregistry;
    requires xorcery.jsonapi;
    requires xorcery.service.api;
    requires xorcery.keystores;

    requires jakarta.inject;
    requires jakarta.validation;
    requires jakarta.annotation;
    requires jakarta.activation;
    requires jakarta.ws.rs;
    requires org.eclipse.jetty.server;

    requires org.glassfish.hk2.api;
    requires org.glassfish.hk2.runlevel;

    requires io.dropwizard.metrics.jetty11;

    requires org.eclipse.jetty.servlet;
    requires org.eclipse.jetty.alpn.java.server;
    requires org.eclipse.jetty.websocket.jetty.server;

    requires org.apache.logging.log4j;
    requires org.eclipse.jetty.http2.server;
    requires jersey.server;
    requires jersey.container.servlet.core;
    requires com.codahale.metrics;

    requires jjwt.api;
}