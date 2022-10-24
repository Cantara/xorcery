open module xorcery.eventstore {
    requires xorcery.reactivestreams.api;
    requires xorcery.configuration.api;
    requires xorcery.service.api;
    requires xorcery.conductor.api;
    requires xorcery.jsonapi.server;
    requires xorcery.domainevents;

    requires jakarta.inject;
    requires jakarta.ws.rs;
    requires db.client.java;
    requires com.fasterxml.jackson.databind;
    requires org.apache.logging.log4j;
    requires xorcery.util;
    requires com.lmax.disruptor;
    requires jersey.server;
    requires jersey.common;
}