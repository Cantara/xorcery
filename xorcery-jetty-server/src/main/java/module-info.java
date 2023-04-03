open module xorcery.jetty.server {
    exports com.exoreaction.xorcery.service.jetty.server;
    exports com.exoreaction.xorcery.service.jetty.server.security;
    exports com.exoreaction.xorcery.service.jetty.server.security.jwt;
    exports com.exoreaction.xorcery.service.jetty.server.security.clientcert;

    requires transitive xorcery.configuration.api;
    requires transitive xorcery.metricregistry.hk2;
    requires xorcery.keystores;
    requires xorcery.keystores.hk2;

    requires jakarta.inject;
    requires jakarta.validation;
    requires jakarta.annotation;
    requires jakarta.activation;

    requires org.glassfish.hk2.api;
    requires org.glassfish.hk2.runlevel;

    requires io.dropwizard.metrics.jetty11;

    requires org.eclipse.jetty.server;
    requires org.eclipse.jetty.alpn.java.server;
    requires org.eclipse.jetty.http2.server;
    requires org.eclipse.jetty.servlet;
    requires org.eclipse.jetty.websocket.jetty.server;

    requires org.apache.logging.log4j;
    requires com.codahale.metrics;

    requires jjwt.api;
}