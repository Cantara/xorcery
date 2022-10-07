open module xorcery.test {
    requires xorcery.server;
    requires xorcery.neo4j;
    requires xorcery.reactivestreams;
    requires xorcery.config;
    requires xorcery.metadata;

    requires org.junit.jupiter.api;

    requires java.base;

    requires jersey.common;
}