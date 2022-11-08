import com.exoreaction.xorcery.service.domainevents.neo4jprojections.ApplyJsonDomainEvent;
import com.exoreaction.xorcery.service.domainevents.neo4jprojections.JsonDomainEventNeo4jEventProjection;
import com.exoreaction.xorcery.service.neo4j.spi.Neo4jProvider;
import com.exoreaction.xorcery.service.neo4jprojections.spi.Neo4jEventProjection;

open module xorcery.domainevents.neo4j {
    exports com.exoreaction.xorcery.service.domainevents.neo4jprojections;
    exports com.exoreaction.xorcery.service.domainevents.snapshot;

    requires xorcery.neo4j.shaded;
    requires xorcery.neo4j;
    requires xorcery.domainevents;

    requires org.glassfish.hk2.api;
    requires org.apache.logging.log4j;

    provides Neo4jProvider with ApplyJsonDomainEvent;
    provides Neo4jEventProjection with JsonDomainEventNeo4jEventProjection;
}