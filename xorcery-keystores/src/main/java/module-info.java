open module xorcery.keystores {
    exports com.exoreaction.xorcery.service.keystores;

    requires xorcery.configuration.api;
    requires org.apache.logging.log4j;
    requires org.bouncycastle.provider;
}