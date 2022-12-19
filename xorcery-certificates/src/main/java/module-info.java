open module xorcery.certificates {
    exports com.exoreaction.xorcery.service.certificates;

    requires xorcery.configuration.api;
    requires jakarta.inject;
    requires org.glassfish.hk2.api;
    requires org.apache.logging.log4j;
    requires org.bouncycastle.provider;
}