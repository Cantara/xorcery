open module xorcery.config {
    exports com.exoreaction.xorcery.configuration;
    exports com.exoreaction.xorcery.configuration.builder;

    requires transitive xorcery.json;

    requires com.fasterxml.jackson.dataformat.javaprop;
    requires com.fasterxml.jackson.dataformat.yaml;

    requires org.apache.logging.log4j;
    requires xorcery.util;
}