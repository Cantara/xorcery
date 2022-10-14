open module xorcery.reactivestreams.test {
    requires xorcery.reactivestreams;
    requires xorcery.config;

    requires jakarta.validation;
    requires org.junit.jupiter.api;
    requires org.eclipse.jetty.server;
    requires org.eclipse.jetty.servlet;
    requires org.eclipse.jetty.websocket.jetty.client;
    requires jersey.container.servlet.core;
    requires jersey.server;
    requires org.eclipse.jetty.websocket.jetty.server;
    requires org.eclipse.jetty.http2.server;
    requires org.eclipse.jetty.http2.http.client.transport;
    requires jakarta.inject;
    requires jakarta.ws.rs;
}