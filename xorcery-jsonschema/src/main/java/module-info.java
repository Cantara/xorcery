open module xorcery.jsonschema {
    exports com.exoreaction.xorcery.hyperschema.model;
    exports com.exoreaction.xorcery.jsonschema.model;
    exports com.exoreaction.xorcery.jsonschema.jaxrs;

    requires transitive xorcery.json;
    requires transitive jakarta.ws.rs;

    requires xorcery.util;
}