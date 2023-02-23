open module xorcery.metadata {
    exports com.exoreaction.xorcery.metadata;

    requires transitive xorcery.configuration.api;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.annotation;
    requires xorcery.util;
}