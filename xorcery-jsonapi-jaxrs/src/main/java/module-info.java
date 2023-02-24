open module xorcery.jsonapi.jaxrs {
    exports com.exoreaction.xorcery.jsonapi.jaxrs.providers;

    requires transitive xorcery.jsonapi;
    requires transitive jakarta.inject;
    requires com.fasterxml.jackson.dataformat.yaml;
    requires jakarta.ws.rs;
}