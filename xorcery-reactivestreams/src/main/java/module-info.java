open module xorcery.reactivestreams {
    requires transitive xorcery.reactivestreams.api;

    requires xorcery.service.api;
    requires xorcery.util;
    requires xorcery.metadata;
    requires xorcery.restclient;

    requires com.fasterxml.jackson.databind;
    requires jakarta.ws.rs;
    requires org.apache.logging.log4j;
    requires org.eclipse.jetty.io;
    requires org.eclipse.jetty.websocket.jetty.api;
    requires org.eclipse.jetty.websocket.jetty.client;
    requires jakarta.inject;
    requires org.eclipse.jetty.server;
    requires org.eclipse.jetty.servlet;
    requires jersey.jetty.connector;
    requires jersey.common;
    requires com.lmax.disruptor;
    requires org.eclipse.jetty.websocket.jetty.server;
}