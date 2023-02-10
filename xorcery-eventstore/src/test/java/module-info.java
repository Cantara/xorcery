open module xorcery.eventstore.test {
    requires xorcery.core.test;
    requires xorcery.eventstore;
    requires xorcery.reactivestreams.server;
    requires xorcery.jetty.client;
    requires org.junit.jupiter.api;
    requires testcontainers;
    requires org.slf4j;
    requires db.client.java;
    requires junit.jupiter;
    requires junit;
    requires jakarta.activation;
    requires jakarta.annotation;
    requires org.glassfish.hk2.api;
}
