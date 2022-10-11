open module xorcery.registry {
    exports com.exoreaction.xorcery.service.registry;
    exports com.exoreaction.xorcery.service.registry.api;
    exports com.exoreaction.xorcery.service.registry.resources.api;

    requires xorcery.json;
    requires xorcery.config;
    requires xorcery.util;
    requires xorcery.restclient;
    requires xorcery.reactivestreams.api;
    requires xorcery.jsonapi.jaxrs;
    requires xorcery.jsonapi.server;
    requires xorcery.jsonapi.client;
    requires xorcery.service.api;

    requires com.fasterxml.jackson.databind;
    requires jersey.common;
    requires jakarta.inject;
    requires jakarta.ws.rs;
    requires org.glassfish.hk2.api;
    requires org.apache.logging.log4j;
    requires jersey.server;
    requires javax.jmdns;
    requires com.lmax.disruptor;
    requires jakarta.annotation;
    requires jersey.client;
    requires jersey.jetty.connector;
    requires java.logging;
}