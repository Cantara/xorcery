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
package com.exoreaction.xorcery.service.neo4j.client;

import com.exoreaction.xorcery.util.Enums;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.neo4j.graphdb.Result;

import java.util.Collection;
import java.util.function.Function;

public record RowModel(Result.ResultRow row) {

    public JsonNode getJsonNode(String name) {
        return Cypher.toJsonNode(row.get(name));
    }

    public NodeModel getNode(String name) {
        return new NodeModel(row.getNode(name));
    }

    public NodeModel getNode(Enum<?> name) {
        return new NodeModel(row.getNode(name.name()));
    }

    public <T> T toModel(Function<ObjectNode, T> creator, Enum<?>... fields) {
        ObjectNode json = JsonNodeFactory.instance.objectNode();
        for (Enum<?> enumConstant : fields) {
            json.set(Enums.toField(enumConstant), this.getJsonNode(Enums.toField(enumConstant)));
        }
        return creator.apply(json);
    }
}
