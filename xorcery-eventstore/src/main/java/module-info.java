open module xorcery.eventstore {
    requires xorcery.metadata;
    requires xorcery.jsonapi;
    requires xorcery.jsonschema;
    requires xorcery.registry;
    requires xorcery.jsonapi.server;
    requires xorcery.reactivestreams.api;
    requires xorcery.domainevents;
    requires xorcery.service.api;
    requires xorcery.conductor.api;

    requires jakarta.inject;
    requires jakarta.ws.rs;
    requires db.client.java;
    requires xorcery.config.api;
    requires com.fasterxml.jackson.databind;
    requires org.apache.logging.log4j;
    requires xorcery.util;
    requires com.lmax.disruptor;
    requires jersey.server;
    requires jersey.common;
}