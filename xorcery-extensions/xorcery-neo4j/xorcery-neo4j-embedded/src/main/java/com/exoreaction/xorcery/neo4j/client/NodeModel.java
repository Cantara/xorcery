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
package com.exoreaction.xorcery.neo4j.client;

import com.exoreaction.xorcery.collections.Element;
import com.fasterxml.jackson.databind.JsonNode;
import org.neo4j.graphdb.Node;

import java.util.Optional;

public record NodeModel(Node node)
    implements Element
{
    @Override
    public Optional<Object> get(String name) {
        return Optional.ofNullable(node.getProperty(name, null));
    }

    @Override
    public boolean has(String name) {
        return node.hasProperty(name);
    }

    public JsonNode getJsonNode(String name) {
        return Cypher.toJsonNode(node.getProperty(name, null));
    }
}
