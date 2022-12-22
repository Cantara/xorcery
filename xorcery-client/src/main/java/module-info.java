open module xorcery.client {
    exports com.exoreaction.xorcery.service.jetty.client;
    exports com.exoreaction.xorcery.service.jetty.client.dns;
    exports com.exoreaction.xorcery.service.jersey.client;

    requires xorcery.configuration.api;
    requires xorcery.keystores;
    requires xorcery.dns.client;

    requires org.apache.logging.log4j;
    requires org.eclipse.jetty.client;
    requires jersey.jetty.connector;
    requires org.eclipse.jetty.http2.client;
    requires org.eclipse.jetty.http2.http.client.transport;
    requires org.glassfish.hk2.api;
    requires jakarta.ws.rs;
    requires jersey.client;
    requires jersey.common;
    requires java.logging;
    requires org.dnsjava;
    requires jakarta.inject;
}