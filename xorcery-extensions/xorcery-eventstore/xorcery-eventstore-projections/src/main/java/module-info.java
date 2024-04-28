module xorcery.eventstore.projections {
    exports com.exoreaction.xorcery.eventstore.projections;

    requires org.glassfish.hk2.api;
    requires org.glassfish.hk2.runlevel;
    requires org.apache.logging.log4j;
    requires db.client.java;
    requires jakarta.inject;
    requires xorcery.configuration.api;
    requires xorcery.eventstore.client;
}