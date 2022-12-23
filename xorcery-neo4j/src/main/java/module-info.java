
open module xorcery.neo4j {
    uses com.exoreaction.xorcery.service.neo4j.spi.Neo4jProvider;

    exports com.exoreaction.xorcery.service.neo4j.client;
    exports com.exoreaction.xorcery.service.neo4j;
    exports com.exoreaction.xorcery.service.neo4j.spi;

    requires xorcery.configuration.api;
    requires xorcery.service.api;
    requires xorcery.jsonapi.server;

    requires xorcery.neo4j.shaded;
    requires org.apache.logging.log4j;
    requires org.glassfish.hk2.api;
    requires jakarta.inject;
    requires com.fasterxml.jackson.dataformat.yaml;
    requires com.codahale.metrics;
}