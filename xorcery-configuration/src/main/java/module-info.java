open module xorcery.configuration {
    exports com.exoreaction.xorcery.configuration.builder;

    requires transitive xorcery.configuration.api;

    requires com.fasterxml.jackson.dataformat.javaprop;
    requires com.fasterxml.jackson.dataformat.yaml;
    requires org.apache.logging.log4j;
}
