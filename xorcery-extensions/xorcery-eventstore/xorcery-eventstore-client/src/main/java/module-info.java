module xorcery.eventstore.client {
    exports dev.xorcery.eventstore;
    exports dev.xorcery.eventstore.client.api;

    requires xorcery.util;
    requires xorcery.metadata;
    requires xorcery.opentelemetry.api;
    requires xorcery.reactivestreams.api;

    requires db.client.java;
    requires org.reactivestreams;
    requires reactor.core;

    requires com.lmax.disruptor;
    requires com.fasterxml.jackson.databind;
    requires grpc.shaded.jpms;
    requires io.opentelemetry.semconv;

    requires jakarta.inject;
    requires org.glassfish.hk2.api;
    requires org.apache.logging.log4j;
    requires xorcery.reactivestreams.extras;
    requires io.opentelemetry.context;
}