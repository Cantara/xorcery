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
package com.exoreaction.xorcery.opensearch.client.search;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public record SearchRequest(ObjectNode json) {

    public record Builder(ObjectNode request) {
        public Builder() {
            this(JsonNodeFactory.instance.objectNode());
        }

        public Builder query(ObjectNode json) {
            request.set("query", json);
            return this;
        }

        public Builder size(int value) {
            request.set("size", request.numberNode(value));
            return this;
        }

        public SearchRequest build() {
            return new SearchRequest(request);
        }
    }
}
