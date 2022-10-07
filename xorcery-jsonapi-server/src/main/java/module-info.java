open module xorcery.jsonapi.server {
    exports com.exoreaction.xorcery.jsonapi.server.resources;
    exports com.exoreaction.xorcery.jsonschema.server.annotations;

    requires transitive xorcery.jsonapi;
    requires xorcery.util;

    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires jakarta.ws.rs;
    requires org.apache.commons.lang3;
    requires jakarta.inject;
    requires com.fasterxml.jackson.dataformat.yaml;
    requires org.apache.logging.log4j;
    requires org.glassfish.hk2.api;
}