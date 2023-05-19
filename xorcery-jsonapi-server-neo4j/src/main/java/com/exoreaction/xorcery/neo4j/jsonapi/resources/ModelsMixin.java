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

import com.exoreaction.xorcery.jsonapi.server.resources.ResourceContext;
import com.exoreaction.xorcery.service.neo4j.client.GraphDatabase;
import com.exoreaction.xorcery.service.neo4j.client.RowModel;
import com.exoreaction.xorcery.util.Enums;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Collection;
import java.util.function.Function;

public interface ModelsMixin
        extends ResourceContext {
    default GraphDatabase database() {
        return service(GraphDatabase.class);
    }

    default <T> Function<RowModel, T> toModel(Function<ObjectNode, T> creator, Collection<Enum<?>> fields) {
        return rowModel -> {
            ObjectNode json = JsonNodeFactory.instance.objectNode();
            for (Enum<?> enumConstant : fields) {
                json.set(Enums.toField(enumConstant), rowModel.getJsonNode(Enums.toField(enumConstant)));
            }
            return creator.apply(json);
        };
    }
}
