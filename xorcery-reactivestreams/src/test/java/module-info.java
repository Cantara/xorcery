open module xorcery.reactivestreams.test {
    requires xorcery.reactivestreams;

    requires xorcery.core;
    requires xorcery.core.test;
    requires xorcery.server;
    requires xorcery.client;

    requires org.glassfish.hk2.api;
    requires com.fasterxml.jackson.datatype.jsr310;
}