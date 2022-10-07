open module xorcery.handlebars {
    requires xorcery.jsonapi.client;
    requires xorcery.restclient;

    requires handlebars;
    requires jakarta.ws.rs;
    requires jersey.common;
    requires jakarta.inject;
    requires org.apache.logging.log4j;
    requires org.eclipse.jetty.client;
    requires com.fasterxml.jackson.databind;
    requires jersey.client;
    requires jersey.jetty.connector;
    requires jersey.server;
    requires java.logging;
}