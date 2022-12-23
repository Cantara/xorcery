open module xorcery.handlebars {
    exports com.exoreaction.xorcery.service.handlebars.jaxrs.providers;
    exports com.exoreaction.xorcery.service.handlebars.helpers;
    exports com.exoreaction.xorcery.service.handlebars;
    exports com.exoreaction.xorcery.service.handlebars.resources.api;

    requires xorcery.configuration.api;
    requires xorcery.jsonschema;
    requires xorcery.jsonapi.client;
    requires xorcery.jsonapi.jaxrs;

    requires org.glassfish.hk2.api;
    requires handlebars;
    requires org.apache.logging.log4j;
    requires jersey.server;
    requires java.logging;
    requires com.fasterxml.jackson.databind;
    requires jakarta.inject;
    requires jakarta.ws.rs;
}