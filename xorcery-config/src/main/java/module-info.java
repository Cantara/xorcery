open module xorcery.config {
    exports com.exoreaction.xorcery.configuration;

    requires transitive xorcery.json;
    requires transitive com.fasterxml.jackson.dataformat.javaprop;
    requires transitive com.fasterxml.jackson.dataformat.yaml;

    requires org.apache.logging.log4j;
}