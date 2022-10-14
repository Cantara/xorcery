open module xorcery.handlebars {
    exports com.exoreaction.xorcery.service.handlebars.jaxrs.providers;
    exports com.exoreaction.xorcery.service.handlebars.helpers;
    exports com.exoreaction.xorcery.service.handlebars.resources;
    exports com.exoreaction.xorcery.service.handlebars;

    requires xorcery.jsonschema;
    requires xorcery.json;
    requires xorcery.jsonapi;
    requires xorcery.restclient;
    requires xorcery.jsonapi.client;
    requires xorcery.jsonapi.jaxrs;

    requires handlebars;
    requires org.apache.logging.log4j;
    requires jersey.client;
    requires jersey.jetty.connector;
    requires jersey.server;
    requires jersey.common;
    requires java.logging;
    requires com.fasterxml.jackson.databind;
    requires jakarta.inject;
    requires jakarta.ws.rs;
}