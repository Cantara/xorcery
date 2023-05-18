open module xorcery.keystores {
    exports com.exoreaction.xorcery.service.keystores;

    requires xorcery.configuration.api;
    requires org.apache.logging.log4j;
    requires org.bouncycastle.provider;
    requires org.glassfish.hk2.api;
    requires jakarta.inject;
    requires org.bouncycastle.pkix;
}