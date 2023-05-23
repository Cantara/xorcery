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
package com.exoreaction.xorcery.opensearch.client.document;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record IndexBulkRequest(List<ObjectNode> requests) {

    public record Builder(List<ObjectNode> requests) {
        public Builder() {
            this(new ArrayList<>());
        }

        public Builder create(String id, ObjectNode json) {
            id = id == null ? UUID.randomUUID().toString() : id;
            requests.add(JsonNodeFactory.instance.objectNode().set("create", JsonNodeFactory.instance.objectNode().set("_id", JsonNodeFactory.instance.textNode(id))));
            requests.add(json);
            return this;
        }

        public IndexBulkRequest build() {
            return new IndexBulkRequest(requests);
        }
    }
}
