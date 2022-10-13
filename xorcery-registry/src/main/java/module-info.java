open module xorcery.registry {
    exports com.exoreaction.xorcery.service.registry;
    exports com.exoreaction.xorcery.service.registry.api;
    exports com.exoreaction.xorcery.service.registry.resources.api;

    requires xorcery.json;
    requires xorcery.config.api;
    requires xorcery.util;
    requires xorcery.restclient;
    requires xorcery.reactivestreams.api;
    requires xorcery.jsonapi.jaxrs;
    requires xorcery.jsonapi.server;
    requires xorcery.jsonapi.client;
    requires xorcery.service.api;

    requires transitive jakarta.annotation;
    requires transitive jakarta.inject;
    requires transitive jakarta.ws.rs;
    requires transitive jersey.common;

    requires com.fasterxml.jackson.databind;
    requires org.glassfish.hk2.api;
    requires org.apache.logging.log4j;
    requires jersey.server;
    requires javax.jmdns;
    requires com.lmax.disruptor;
    requires jersey.client;
    requires jersey.jetty.connector;
    requires java.logging;
}