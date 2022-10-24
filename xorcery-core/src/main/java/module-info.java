open module xorcery.core {
    exports com.exoreaction.xorcery.core;

    requires transitive xorcery.configuration;
    requires transitive jakarta.inject;
    requires transitive org.apache.logging.log4j;

    requires org.glassfish.hk2.api;
    requires com.fasterxml.jackson.dataformat.yaml;
    requires info.picocli;
}