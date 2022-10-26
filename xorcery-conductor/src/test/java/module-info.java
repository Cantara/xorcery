open module xorcery.conductor.test {
    requires xorcery.conductor;

    requires xorcery.core.test;
    requires xorcery.core;
    requires xorcery.registry.api;
    requires xorcery.registry;
    requires xorcery.reactivestreams;
    requires xorcery.client;
    requires xorcery.server;

    requires jakarta.activation;

    requires org.apache.logging.log4j;
    requires org.junit.jupiter.api;
    requires org.hamcrest;
}