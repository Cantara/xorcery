open module xorcery.jsonapi {
    exports com.exoreaction.xorcery.jsonapi;
    exports com.exoreaction.xorcery.jsonapi.model;
    exports com.exoreaction.xorcery.jsonapischema.model;
    exports com.exoreaction.xorcery.server.model;

    requires transitive xorcery.jsonschema;
    requires xorcery.configuration.api;

    requires com.fasterxml.jackson.core;
    requires jersey.common;
}