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
package dev.xorcery.graphql.server.schema;

import com.fasterxml.jackson.databind.JsonNode;
import dev.xorcery.configuration.Configuration;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public record GraphQLSchemasConfiguration(Configuration configuration) {

    public Optional<SchemaConfiguration> getSchemaConfiguration(String name)
    {
        Configuration schemaConfiguration = configuration.getConfiguration(name);
        if (schemaConfiguration.json().isEmpty())
            return Optional.empty();
        else
            return Optional.of(new SchemaConfiguration(schemaConfiguration));
    }

    public record SchemaConfiguration(Configuration configuration)
    {
        public List<String> getResources()
        {
            return configuration.getListAs("resources", JsonNode::asText).orElse(Collections.emptyList());
        }
    }
}
