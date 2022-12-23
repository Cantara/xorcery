open module xorcery.service.api {
    exports com.exoreaction.xorcery.server.model;
    exports com.exoreaction.xorcery.server.api;

    requires transitive xorcery.configuration.api;
    requires transitive xorcery.jsonapi;

    requires transitive jakarta.ws.rs;
    requires org.glassfish.hk2.api;

    requires jakarta.inject;
}