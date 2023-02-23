open module xorcery.configuration.api {
    exports com.exoreaction.xorcery.configuration.model;

    requires transitive xorcery.json;
    requires transitive xorcery.util;
}