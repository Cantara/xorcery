open module xorcery.certificates.client.hk2 {
    exports com.exoreaction.xorcery.service.certificates.client.hk2;

    requires xorcery.certificates.client;

    requires org.eclipse.jetty.client;
    requires jakarta.inject;
    requires org.glassfish.hk2.runlevel;
    requires org.glassfish.hk2.api;
    requires xorcery.keystores;
    requires xorcery.configuration.api;
    requires xorcery.dns.client;
}