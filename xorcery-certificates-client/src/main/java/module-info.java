open module xorcery.certificates.client {
    exports com.exoreaction.xorcery.service.certificates.client;

    requires org.eclipse.jetty.client;

    requires org.apache.logging.log4j;

    requires org.bouncycastle.pkix;
    requires org.bouncycastle.provider;
    requires org.bouncycastle.util;
    requires xorcery.configuration.api;
    requires xorcery.jetty.client;
    requires xorcery.keystores;
    requires xorcery.jsonapi;
}