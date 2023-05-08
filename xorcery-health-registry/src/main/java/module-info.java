open module xorcery.health.registry {
    requires transitive xorcery.health.api;
    requires com.codahale.metrics.health;
    requires org.apache.logging.log4j;
    requires xorcery.configuration.api;
    requires jakarta.inject;
    requires org.glassfish.hk2.api;

    exports com.exoreaction.xorcery.health.registry;
}