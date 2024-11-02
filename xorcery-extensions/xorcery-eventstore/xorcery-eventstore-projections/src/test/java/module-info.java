open module xorcery.eventstore.projections.test {
    exports dev.xorcery.eventstore.projections.test;

    requires xorcery.eventstore.projections;
    requires junit.jupiter;
    requires org.apache.logging.log4j;
    requires testcontainers;
    requires org.junit.jupiter.api;
    requires xorcery.configuration.api;
    requires xorcery.configuration;
    requires xorcery.core;
    requires db.client.java;
    requires xorcery.eventstore.client;
    requires org.slf4j;
    requires junit;
}