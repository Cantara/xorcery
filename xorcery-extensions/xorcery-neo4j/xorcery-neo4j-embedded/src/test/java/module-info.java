open module xorcery.neo4j.embedded.test {
    requires xorcery.neo4j.embedded;
    requires xorcery.junit;
    requires xorcery.neo4j.shaded;
    requires xorcery.opentelemetry.sdk;

    requires org.junit.jupiter.api;
    requires io.opentelemetry.sdk.metrics;
}