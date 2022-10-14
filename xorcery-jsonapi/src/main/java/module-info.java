open module xorcery.jsonapi {
    exports com.exoreaction.xorcery.jsonapi;
    exports com.exoreaction.xorcery.jsonapi.model;
    exports com.exoreaction.xorcery.jsonapischema.model;

    requires transitive xorcery.jsonschema;
    requires transitive jersey.common;

    requires com.fasterxml.jackson.core;
}