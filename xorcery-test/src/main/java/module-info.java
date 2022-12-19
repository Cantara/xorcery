module xorcery.test {
    exports com.exoreaction.xorcery.service.dns;

    requires xorcery.core;
    requires xorcery.service.api;
    requires xorcery.reactivestreams.api;
    requires xorcery.neo4j;
    requires jakarta.inject;
    requires org.glassfish.hk2.runlevel;
}