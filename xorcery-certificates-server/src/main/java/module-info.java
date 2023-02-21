open module xorcery.certificates.server {
    exports com.exoreaction.xorcery.service.certificates.server;
    exports com.exoreaction.xorcery.service.certificates.server.resources.api;

    requires xorcery.keystores;
    requires xorcery.keystores.hk2;
    requires xorcery.jsonapi.server;
    requires xorcery.jsonapi.jaxrs;
    requires xorcery.service.api;
    requires xorcery.configuration.api;

    requires jakarta.inject;
    requires org.apache.logging.log4j;
    requires java.logging;
    requires org.glassfish.hk2.api;
    requires org.glassfish.hk2.runlevel;

    requires org.bouncycastle.provider;
    requires org.bouncycastle.pkix;
}