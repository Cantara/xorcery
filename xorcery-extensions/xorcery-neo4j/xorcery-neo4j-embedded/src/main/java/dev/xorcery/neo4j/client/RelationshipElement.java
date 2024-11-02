package dev.xorcery.neo4j.client;

import org.neo4j.graphdb.Entity;
import org.neo4j.graphdb.Relationship;

public interface RelationshipElement
        extends EntityElement
{
    Relationship relationship();

    @Override
    default Entity entity()
    {
        return relationship();
    }
}
