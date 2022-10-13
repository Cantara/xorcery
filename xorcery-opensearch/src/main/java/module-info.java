open module xorcery.opensearch {
    requires xorcery.json;
    requires com.fasterxml.jackson.databind;
    requires jakarta.ws.rs;
    requires xorcery.config.api;
    requires xorcery.service.api;
    requires xorcery.conductor.api;
    requires xorcery.reactivestreams.api;
    requires com.fasterxml.jackson.dataformat.yaml;
    requires jakarta.inject;
    requires org.apache.logging.log4j;
    requires org.eclipse.jetty.server;
    requires jersey.client;
    requires jersey.jetty.connector;
    requires jersey.common;
    requires jersey.server;
    requires java.logging;
    requires xorcery.jsonapi;
    requires xorcery.restclient;
    requires xorcery.disruptor;
    requires xorcery.metadata;
    requires xorcery.domainevents;
    requires com.lmax.disruptor;
    requires xorcery.util;
    requires xorcery.jsonapi.jaxrs;
}