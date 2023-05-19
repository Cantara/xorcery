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
package com.exoreaction.xorcery.service.neo4jprojections;

import com.exoreaction.xorcery.json.model.JsonElement;
import com.exoreaction.xorcery.service.neo4j.client.Cypher;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Optional;

import static com.exoreaction.xorcery.service.neo4j.client.Cypher.toField;

public record ProjectionModel(ObjectNode json)
        implements JsonElement {
    public String getId() {
        return getString(toField(Projection.id)).orElseThrow();
    }

    public Optional<Long> getVersion() {
        return getLong(toField(Projection.version));
    }

    public Optional<Long> getRevision() {
        return getLong(toField(Projection.revision));
    }
}
