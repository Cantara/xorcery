open module xorcery.service.api {
    exports com.exoreaction.xorcery.jersey;
    exports com.exoreaction.xorcery.server.model;

    requires transitive xorcery.config.api;

    requires transitive jakarta.ws.rs;
    requires transitive jersey.common;
    requires transitive xorcery.jsonapi;
}