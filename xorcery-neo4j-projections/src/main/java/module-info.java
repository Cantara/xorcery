import com.exoreaction.xorcery.service.neo4jprojections.spi.Neo4jEventProjection;
import com.exoreaction.xorcery.service.neo4jprojections.streams.CypherEventProjection;

open module xorcery.neo4j.projections {
    uses com.exoreaction.xorcery.service.neo4jprojections.spi.Neo4jEventProjection;

    exports com.exoreaction.xorcery.service.neo4jprojections.api;
    exports com.exoreaction.xorcery.service.neo4jprojections.spi;

    requires xorcery.service.api;
    requires xorcery.neo4j;
    requires xorcery.neo4j.shaded;
    requires xorcery.jsonapi.server;
    requires xorcery.reactivestreams.api;
    requires xorcery.disruptor;

    requires org.glassfish.hk2.api;
    requires jakarta.inject;
    requires com.codahale.metrics;
    requires org.apache.logging.log4j;
    requires org.glassfish.hk2.runlevel;
    requires xorcery.metricregistry.hk2;

    provides Neo4jEventProjection with CypherEventProjection;
}