open module xorcery.certificates.letsencrypt {
    exports com.exoreaction.xorcery.service.certificates.letsencrypt;
    exports com.exoreaction.xorcery.service.certificates.letsencrypt.resources;

    requires xorcery.certificates;
    requires xorcery.certificates.server;
    requires xorcery.keystores;
    requires xorcery.util;
    requires xorcery.configuration.api;

    requires jakarta.ws.rs;
    requires jakarta.inject;
    requires org.glassfish.hk2.api;
    requires org.bouncycastle.provider;
    requires org.shredzone.acme4j;
    requires org.apache.logging.log4j;
    requires org.shredzone.acme4j.utils;
    requires info.picocli;
    requires org.bouncycastle.pkix;
    requires java.base;
}