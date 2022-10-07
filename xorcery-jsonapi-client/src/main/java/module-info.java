open module xorcery.jsonapi.client {
    exports com.exoreaction.xorcery.jsonapi.client;
    exports com.exoreaction.xorcery.hyperschema.client;

    requires transitive xorcery.jsonapi;

    requires com.fasterxml.jackson.core;
    requires org.apache.commons.lang3;
    requires jakarta.inject;
    requires com.fasterxml.jackson.dataformat.yaml;
    requires org.apache.logging.log4j;
}