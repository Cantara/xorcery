open module xorcery.health.registry.hk2 {
    requires xorcery.configuration.api;
    requires xorcery.health.api;
    requires xorcery.health.registry;

    requires org.glassfish.hk2.api;
    requires jakarta.inject;
    requires com.codahale.metrics.health;
}