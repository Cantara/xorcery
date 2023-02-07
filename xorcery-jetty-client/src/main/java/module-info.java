open module xorcery.jetty.client {
    exports com.exoreaction.xorcery.service.jetty.client;

    requires xorcery.configuration.api;
    requires xorcery.keystores;
    requires xorcery.dns.client;

    requires org.eclipse.jetty.client;
    requires jakarta.inject;
    requires org.glassfish.hk2.api;

    requires org.apache.logging.log4j;
    requires org.eclipse.jetty.http2.http.client.transport;
}