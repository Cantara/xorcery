open module xorcery.service.api {
    exports com.exoreaction.xorcery.jersey;
    exports com.exoreaction.xorcery.model;
    exports com.exoreaction.xorcery.server.model;

    requires transitive xorcery.config;

    requires jakarta.ws.rs;
    requires jersey.common;
    requires xorcery.jsonapi;
    requires org.apache.logging.log4j;
}