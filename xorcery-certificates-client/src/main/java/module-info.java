open module xorcery.certificates.client {
    exports com.exoreaction.xorcery.service.certificates.client;

    requires xorcery.keystores;
    requires xorcery.service.api;

    requires jakarta.inject;
    requires org.apache.logging.log4j;
    requires java.logging;
    requires org.glassfish.hk2.runlevel;

    requires org.bouncycastle.pkix;
    requires org.bouncycastle.provider;
    requires org.bouncycastle.util;
    requires xorcery.configuration.api;
    requires org.glassfish.hk2.api;
}