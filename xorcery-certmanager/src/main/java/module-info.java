open module xorcery.certmanager {
    requires xorcery.conductor.api;
    requires xorcery.jsonapi.server;
    requires xorcery.jsonapi.client;
    requires xorcery.jsonapi.jaxrs;

    requires jakarta.inject;
    requires org.apache.logging.log4j;
    requires jersey.server;
    requires jersey.client;
    requires org.eclipse.jetty.util;
    requires jersey.jetty.connector;
    requires java.logging;
}