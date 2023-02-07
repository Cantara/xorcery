open module xorcery.reactivestreams.server.test {
    requires xorcery.reactivestreams.client;
    requires xorcery.reactivestreams.server;

    requires xorcery.core;
    requires xorcery.core.test;
    requires xorcery.jetty.client;
    requires xorcery.jetty.server;

    requires jakarta.ws.rs;
    requires org.glassfish.hk2.api;
    requires com.fasterxml.jackson.datatype.jsr310;
}