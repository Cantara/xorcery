open module xorcery.jersey.client {
    exports com.exoreaction.xorcery.service.jersey.client;

    requires xorcery.configuration.api;
    requires xorcery.keystores;
    requires xorcery.dns.client;
    requires xorcery.jetty.client;

    requires org.apache.logging.log4j;

    requires org.glassfish.hk2.api;
    requires jakarta.ws.rs;
    requires jakarta.inject;
    requires org.eclipse.jetty.client;
    requires jersey.jetty.connector;
    requires jersey.client;
    requires jersey.common;
    requires java.logging;
    requires org.dnsjava;
}