package com.exoreaction.xorcery.neo4j.jsonapi.resources;

import com.exoreaction.xorcery.domainevents.jsonapi.resources.CommandsMixin;
import com.exoreaction.xorcery.jsonapi.server.resources.IncludesMixin;
import com.exoreaction.xorcery.jsonapi.server.resources.RelationshipsMixin;
import com.exoreaction.xorcery.jsonschema.server.resources.JsonSchemaMixin;

/**
 * Helper methods for JSON:API resource implementations backed by Neo4j
 */
public interface JsonApiNeo4jResourceMixin
    extends ModelsMixin, RelationshipsMixin, CommandsMixin,
        FilteringMixin, FieldsMixin, IncludesMixin, SortingMixin, PagingMixin,
        JsonSchemaMixin
{
}
