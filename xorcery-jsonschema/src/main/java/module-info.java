open module xorcery.jsonschema {
    exports com.exoreaction.xorcery.hyperschema.model;
    exports com.exoreaction.xorcery.jsonschema.model;
    exports com.exoreaction.xorcery.jsonschema.jaxrs;

    requires transitive xorcery.json;
    requires transitive xorcery.util;
    requires transitive jakarta.ws.rs;
}