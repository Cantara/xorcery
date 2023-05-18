open module xorcery.certificates {
    exports com.exoreaction.xorcery.service.certificates.spi;
    exports com.exoreaction.xorcery.service.certificates;

    requires org.apache.logging.log4j;

    requires org.bouncycastle.pkix;
    requires org.bouncycastle.provider;
    requires org.bouncycastle.util;
    requires xorcery.configuration.api;
    requires xorcery.keystores;
    requires xorcery.dns.client;
    requires org.glassfish.hk2.api;
    requires org.glassfish.hk2.runlevel;
    requires jakarta.inject;
}