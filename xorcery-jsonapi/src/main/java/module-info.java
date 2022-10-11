open module xorcery.jsonapi {
    exports com.exoreaction.xorcery.jsonapi;
    exports com.exoreaction.xorcery.jsonapi.model;
    exports com.exoreaction.xorcery.jsonapischema.model;

    requires transitive xorcery.jsonschema;

    requires com.fasterxml.jackson.core;
}