open module xorcery.eventstore {
    exports com.exoreaction.xorcery.service.eventstore;
    requires xorcery.reactivestreams.api;
    requires xorcery.configuration.api;
    requires xorcery.service.api;
    requires xorcery.jsonapi.server;

    requires jakarta.inject;
    requires jakarta.ws.rs;
    requires db.client.java;
    requires com.fasterxml.jackson.databind;
    requires org.apache.logging.log4j;
    requires xorcery.util;
    requires com.lmax.disruptor;
    requires jersey.common;
}