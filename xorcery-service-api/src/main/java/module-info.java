open module xorcery.service.api {
    exports com.exoreaction.xorcery.server.model;
    exports com.exoreaction.xorcery.server.spi;

    requires transitive xorcery.configuration.api;
    requires transitive xorcery.jsonapi;

    requires transitive jakarta.ws.rs;
    requires transitive org.glassfish.hk2.api;

    requires jakarta.inject;
}