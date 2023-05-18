open module xorcery.certificates.client {
    exports com.exoreaction.xorcery.service.certificates.client;
    exports com.exoreaction.xorcery.service.certificates.client.resources;

    requires xorcery.certificates;
    requires xorcery.jetty.client;
    requires xorcery.configuration.api;
    requires xorcery.jsonapi;
    requires xorcery.dns.client;
    requires org.eclipse.jetty.client;
    requires org.apache.logging.log4j;

    requires org.bouncycastle.pkix;
    requires org.bouncycastle.provider;
    requires org.bouncycastle.util;
    requires org.glassfish.hk2.api;
    requires org.glassfish.hk2.runlevel;
    requires jakarta.inject;
    requires jakarta.ws.rs;
    requires xorcery.keystores;
}