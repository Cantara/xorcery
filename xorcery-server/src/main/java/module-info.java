open module xorcery.server {
    exports com.exoreaction.xorcery.server;

    requires transitive xorcery.jsonapi.client;
    requires transitive xorcery.config;
    requires transitive xorcery.service.api;
    requires transitive xorcery.conductor.api;
    requires transitive jakarta.inject;
    requires transitive jakarta.ws.rs;
    requires transitive jersey.common;
    requires transitive jakarta.validation;

//    requires xorcery.domainevents;
    requires org.apache.commons.lang3;
    requires org.eclipse.jetty.util;
    requires org.apache.logging.log4j;
    requires jersey.server;
    requires info.picocli;
    requires com.codahale.metrics;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.dataformat.yaml;
    requires io.dropwizard.metrics.jetty11;
    requires org.eclipse.jetty.alpn.server;
    requires org.eclipse.jetty.client;
    requires org.eclipse.jetty.http2.client;
    requires org.eclipse.jetty.http2.http.client.transport;
    requires org.eclipse.jetty.http2.server;
    requires org.eclipse.jetty.http3.client;
    requires org.eclipse.jetty.http3.http.client.transport;
    requires org.eclipse.jetty.http3.server;
    requires org.eclipse.jetty.servlet;
    requires org.eclipse.jetty.websocket.jetty.server;
    requires org.glassfish.hk2.api;
    requires jersey.jetty.connector;
    requires jersey.container.servlet.core;
    requires java.management;
    requires jdk.management;
    requires jersey.client;
    requires java.logging;
    requires xorcery.jsonapi.server;
    requires xorcery.util;
}