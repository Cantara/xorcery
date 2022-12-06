module xorcery.test {
    exports com.exoreaction.xorcery.service.dns;

    requires xorcery.configuration.api;
    requires xorcery.service.api;
    requires xorcery.neo4j;
    requires jakarta.inject;
    requires org.glassfish.hk2.runlevel;
}