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
package com.exoreaction.xorcery.metadata;

import com.exoreaction.xorcery.builders.With;
import com.exoreaction.xorcery.json.model.JsonElement;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static com.fasterxml.jackson.annotation.JsonCreator.Mode.DELEGATING;


public record Metadata(ObjectNode json)
        implements JsonElement {

    public record Builder(ObjectNode builder)
            implements With<Builder> {

        public Builder() {
            this(JsonNodeFactory.instance.objectNode());
        }

        public Builder add(String name, String value) {
            builder.set(name, builder.textNode(value));
            return this;
        }

        public Builder add(String name, long value) {
            builder.set(name, builder.numberNode(value));
            return this;
        }

        public Builder add(String name, ObjectNode value) {
            builder.set(name, value);
            return this;
        }

        public Builder add(Metadata metadata) {
            this.builder.setAll(metadata.json);
            return this;
        }

        public Metadata build() {
            return new Metadata(builder);
        }
    }

    @JsonCreator(mode = DELEGATING)
    public Metadata {
    }

    @JsonValue
    public ObjectNode metadata() {
        return json;
    }

    public Builder toBuilder() {
        return new Builder(json);
    }

    public Metadata copy() {
        return new Metadata(json.deepCopy());
    }
}
