open module xorcery.test {
    requires xorcery.server;
    requires xorcery.neo4j;
    requires xorcery.reactivestreams;
    requires xorcery.configuration;
    requires xorcery.metadata;

    requires xorcery.conductor;
    requires xorcery.registry;

    requires org.junit.jupiter.api;

    requires java.base;

    requires jersey.common;
}