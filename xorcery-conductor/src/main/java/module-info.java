open module xorcery.conductor {
    exports com.exoreaction.xorcery.service.conductor;

    requires transitive xorcery.jsonapi;
    requires transitive xorcery.conductor.api;
    requires transitive xorcery.config.api;
    requires transitive xorcery.service.api;
    requires xorcery.reactivestreams.api;
    requires xorcery.registry;

    requires jakarta.inject;
    requires jakarta.ws.rs;
    requires com.fasterxml.jackson.core;
    requires org.apache.logging.log4j;
    requires jersey.server;
    requires xorcery.jsonapi.server;
    requires commons.jexl3;
    requires com.fasterxml.jackson.dataformat.yaml;
}