open module xorcery.registry {
    exports com.exoreaction.xorcery.service.registry;
    exports com.exoreaction.xorcery.service.registry.resources.api;

    requires transitive xorcery.registry.api;
    requires xorcery.service.api;
    requires xorcery.jsonapi.jaxrs;
    requires xorcery.jsonapi.client;
    requires xorcery.jsonapi.server;
    requires xorcery.restclient;
    requires xorcery.reactivestreams.api;

    requires transitive jakarta.annotation;
    requires transitive jakarta.inject;
    requires transitive jersey.common;

    requires com.lmax.disruptor;
    requires org.apache.logging.log4j;
    requires jersey.server;
    requires javax.jmdns;
    requires jersey.client;
    requires jersey.jetty.connector;
    requires java.logging;
    requires org.glassfish.hk2.api;
}