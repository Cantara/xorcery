open module xorcery.opensearch.client.test {
    requires xorcery.opensearch.client;
    requires org.slf4j;
    requires jakarta.ws.rs;
    requires jersey.common;
    requires java.logging;
    requires xorcery.junit;
    requires xorcery.reactivestreams.api;
    requires testcontainers;
    requires junit.jupiter;
    requires org.hamcrest;
    requires junit;
    requires org.apache.commons.compress;
}