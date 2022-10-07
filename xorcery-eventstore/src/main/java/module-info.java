open module xorcery.eventstore {
    requires xorcery.metadata;
    requires xorcery.jsonapi;
    requires xorcery.jsonschema;
    requires xorcery.registry;
    requires jakarta.inject;
    requires jakarta.ws.rs;
    requires xorcery.neo4j;
    requires db.client.java;
    requires xorcery.config;
    requires xorcery.reactivestreams.api;
    requires com.fasterxml.jackson.databind;
    requires org.apache.logging.log4j;
    requires xorcery.util;
    requires com.lmax.disruptor;
    requires xorcery.domainevents;
    requires xorcery.service.api;
    requires xorcery.conductor.api;
    requires jersey.server;
    requires jersey.common;
}