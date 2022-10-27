open module xorcery.core {
    exports com.exoreaction.xorcery.core;

    requires transitive xorcery.configuration;
    requires transitive jakarta.inject;
    requires transitive org.apache.logging.log4j;

    requires transitive org.glassfish.hk2.api;
    requires transitive org.glassfish.hk2.extras;
    requires com.fasterxml.jackson.dataformat.yaml;
    requires info.picocli;
}