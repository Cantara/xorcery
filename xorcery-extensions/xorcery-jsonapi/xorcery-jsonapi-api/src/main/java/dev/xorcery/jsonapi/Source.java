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
package dev.xorcery.jsonapi;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.xorcery.builders.With;
import dev.xorcery.json.JsonElement;

/**
 * @author rickardoberg
 */
public record Source(ObjectNode json)
        implements JsonElement {
    public record Builder(ObjectNode builder)
            implements With<Builder>
    {
        public Builder() {
            this(JsonNodeFactory.instance.objectNode());
        }

        public Builder pointer(String value) {
            builder.set("pointer", builder.textNode(value));
            return this;
        }

        public Builder parameter(String value) {
            builder.set("detail", builder.textNode(value));
            return this;
        }

        public Source build() {
            return new Source(builder);
        }
    }

    public String getPointer() {
        return getString("pointer").orElse(null);
    }

    public String getParameter() {
        return getString("parameter").orElse(null);
    }
}
