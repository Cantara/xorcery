module xorcery.secrets.api.test {
    exports dev.xorcery.secrets.test;

    requires xorcery.secrets.api;
    requires xorcery.configuration;
    requires xorcery.configuration.api;
    requires xorcery.core;
    requires org.junit.jupiter.api;
    requires org.glassfish.hk2.api;
    requires org.apache.logging.log4j.core;
}