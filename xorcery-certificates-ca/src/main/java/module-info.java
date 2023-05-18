open module xorcery.certificates.ca {
    exports com.exoreaction.xorcery.service.certificates.ca;
    exports com.exoreaction.xorcery.service.certificates.ca.resources;

    requires xorcery.certificates;
    requires xorcery.keystores;
    requires xorcery.configuration.api;

    requires jakarta.inject;
    requires org.apache.logging.log4j;
    requires org.glassfish.hk2.api;
    requires org.glassfish.hk2.runlevel;

    requires org.bouncycastle.provider;
    requires org.bouncycastle.pkix;
    requires jakarta.ws.rs;
    requires xorcery.service.api;
}