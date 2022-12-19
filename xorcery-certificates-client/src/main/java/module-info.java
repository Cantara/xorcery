open module xorcery.certificates.client {
    exports com.exoreaction.xorcery.service.certificates.client;

    requires xorcery.jsonapi.client;
    requires xorcery.jsonapi.jaxrs;
    requires xorcery.client;

    requires jakarta.inject;
    requires org.apache.logging.log4j;
    requires jersey.client;
    requires org.eclipse.jetty.util;
    requires jersey.jetty.connector;
    requires java.logging;
    requires org.glassfish.hk2.runlevel;

    requires org.bouncycastle.pkix;
    requires org.bouncycastle.provider;
    requires org.bouncycastle.util;
    requires xorcery.configuration.api;
    requires org.glassfish.hk2.api;
    requires xorcery.certificates;
    requires xorcery.service.api;
}