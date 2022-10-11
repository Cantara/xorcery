open module xorcery.metadata {
    exports com.exoreaction.xorcery.metadata;

    requires transitive xorcery.config;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.annotation;
    requires xorcery.util;
}