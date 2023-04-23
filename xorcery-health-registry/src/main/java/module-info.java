open module xorcery.health.registry {
    requires transitive xorcery.health.api;
    requires com.codahale.metrics.health;
    requires org.apache.logging.log4j;

    exports com.exoreaction.xorcery.health.registry;
}