module xorcery.log4j {
    exports dev.xorcery.log4j;

    requires transitive org.apache.logging.log4j;
    requires transitive org.apache.logging.log4j.core;

    requires org.glassfish.hk2.api;
    requires xorcery.configuration.api;
    requires jakarta.inject;
    requires xorcery.core;
}