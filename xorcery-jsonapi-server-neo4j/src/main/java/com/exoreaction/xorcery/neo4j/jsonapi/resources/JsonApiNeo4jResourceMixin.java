/*
 * Copyright © 2022 eXOReaction AS (rickard@exoreaction.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
