open module xorcery.jmxmetrics {
    requires xorcery.config.api;
    requires xorcery.service.api;
    requires xorcery.jsonapi;
    requires xorcery.restclient;
    requires xorcery.conductor.api;
    requires xorcery.reactivestreams.api;
    requires xorcery.jsonapi.client;
    requires xorcery.jsonapi.jaxrs;

    requires com.codahale.metrics;
    requires com.fasterxml.jackson.databind;
    requires com.codahale.metrics.jmx;
    requires com.lmax.disruptor;
    requires jakarta.inject;
    requires jakarta.ws.rs;
    requires org.apache.logging.log4j;
    requires jersey.client;
    requires jersey.jetty.connector;
    requires jersey.common;
    requires jersey.server;
    requires java.management;
    requires java.logging;
}