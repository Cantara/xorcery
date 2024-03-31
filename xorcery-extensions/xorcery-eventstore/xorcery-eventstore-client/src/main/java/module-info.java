module xorcery.eventstore.client {
    exports com.exoreaction.xorcery.eventstore.client;
    exports com.exoreaction.xorcery.eventstore;
    exports com.exoreaction.xorcery.eventstore.client.api;

    requires xorcery.util;
    requires xorcery.metadata;
    requires xorcery.opentelemetry.api;
    requires xorcery.reactivestreams.api;

    requires db.client.java;
    requires reactor.core;
    requires org.apache.logging.log4j;
    requires com.fasterxml.jackson.databind;
    requires grpc.shaded.jpms;
    requires io.opentelemetry.semconv;
    requires org.reactivestreams;
    requires jakarta.inject;
    requires org.glassfish.hk2.api;
}