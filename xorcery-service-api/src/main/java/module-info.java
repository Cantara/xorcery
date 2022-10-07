open module xorcery.service.api {
    requires transitive xorcery.config;

    requires jakarta.ws.rs;
    requires jersey.common;
    requires xorcery.jsonapi;
    requires xorcery.json;
    requires org.apache.logging.log4j;
    exports com.exoreaction.xorcery.jersey;
    exports com.exoreaction.xorcery.server.model;
    exports com.exoreaction.xorcery.model;
}