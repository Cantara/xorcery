open module xorcery.reactivestreams {
    exports com.exoreaction.xorcery.service.reactivestreams;
    requires transitive xorcery.reactivestreams.api;

    requires xorcery.service.api;
    requires xorcery.util;
    requires xorcery.metadata;
    requires xorcery.restclient;

/*
    requires com.fasterxml.jackson.databind;
    requires jakarta.ws.rs;
    requires org.eclipse.jetty.io;
    requires org.eclipse.jetty.websocket.jetty.api;
    requires jersey.common;
*/
    requires org.eclipse.jetty.websocket.jetty.server;
    requires com.lmax.disruptor;
    requires jersey.jetty.connector;
    requires org.eclipse.jetty.servlet;
    requires org.eclipse.jetty.server;
    requires org.eclipse.jetty.websocket.jetty.client;
    requires jakarta.inject;
    requires org.glassfish.hk2.api;
    requires org.apache.logging.log4j;
}