open module xorcery.jsonapi.server {
    exports com.exoreaction.xorcery.jsonapi.server.providers;
    exports com.exoreaction.xorcery.jsonapi.server.resources;
    exports com.exoreaction.xorcery.jsonschema.server.annotations;
    exports com.exoreaction.xorcery.jsonschema.server.resources;

    requires transitive xorcery.jsonapi;

    requires org.apache.logging.log4j;
    requires org.glassfish.hk2.api;
    requires org.apache.commons.lang3;
    requires jakarta.inject;
}