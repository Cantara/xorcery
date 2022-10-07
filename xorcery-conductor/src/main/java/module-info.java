open module xorcery.conductor {
    requires xorcery.jsonapi;
    requires xorcery.conductor.api;
    requires xorcery.config;
    requires xorcery.service.api;
    requires xorcery.reactivestreams.api;
    requires xorcery.registry;

    requires jakarta.inject;
    requires jakarta.ws.rs;
    requires com.fasterxml.jackson.core;
    requires org.apache.logging.log4j;
    requires jersey.server;
    requires xorcery.jsonapi.server;
}