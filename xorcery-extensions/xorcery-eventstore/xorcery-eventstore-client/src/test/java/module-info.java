open module xorcery.eventstore.client.test {
    requires db.client.java;
    requires xorcery.configuration;
    requires xorcery.eventstore.client;
    requires xorcery.junit;
    requires xorcery.metadata;
    requires xorcery.reactivestreams.extras;
    requires org.slf4j;
    requires testcontainers;
    requires junit.jupiter;
    requires reactor.core;
    requires xorcery.log4j;
    requires junit;
    requires org.apache.commons.compress;
    requires org.reactivestreams;
}