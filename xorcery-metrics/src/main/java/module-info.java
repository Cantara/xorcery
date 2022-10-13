open module xorcery.metrics {
    requires xorcery.metadata;
    requires xorcery.reactivestreams.api;
    requires xorcery.service.api;
    requires xorcery.conductor.api;
    requires xorcery.jsonapi.server;

    requires com.fasterxml.jackson.databind;
    requires java.management;
    requires com.codahale.metrics;
    requires org.apache.logging.log4j;
    requires jakarta.inject;
    requires jakarta.ws.rs;
    requires jersey.server;
}