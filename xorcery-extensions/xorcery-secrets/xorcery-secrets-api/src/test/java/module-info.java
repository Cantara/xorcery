module xorcery.secrets.api.test {
    exports com.exoreaction.xorcery.secrets.test;

    requires xorcery.secrets.api;
    requires xorcery.configuration;
    requires xorcery.core;
    requires org.junit.jupiter.api;
    requires org.glassfish.hk2.api;
}