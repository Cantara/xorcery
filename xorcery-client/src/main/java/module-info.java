open module xorcery.client {
    exports com.exoreaction.xorcery.service.jetty.client;
    exports com.exoreaction.xorcery.service.jersey.client;

    requires transitive xorcery.core;

    requires com.fasterxml.jackson.dataformat.yaml;
    requires org.eclipse.jetty.client;
    requires jersey.jetty.connector;
    requires com.codahale.metrics;
    requires org.eclipse.jetty.http2.client;
    requires org.eclipse.jetty.http2.http.client.transport;
    requires org.glassfish.hk2.api;
    requires jakarta.ws.rs;
    requires jersey.client;
    requires xorcery.jsonapi.jaxrs;
    requires java.logging;
    requires org.dnsjava;
}