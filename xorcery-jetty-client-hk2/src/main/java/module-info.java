open module xorcery.jetty.client.hk2 {
    requires xorcery.jetty.client;
    requires org.glassfish.hk2.api;
    requires jakarta.inject;
    requires xorcery.configuration.api;
    requires xorcery.keystores;
    requires org.eclipse.jetty.util;
    requires org.apache.logging.log4j;
    requires xorcery.dns.client;
    requires org.eclipse.jetty.client;
}