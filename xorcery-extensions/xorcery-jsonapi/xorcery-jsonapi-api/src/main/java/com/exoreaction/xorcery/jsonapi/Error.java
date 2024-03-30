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
package com.exoreaction.xorcery.jsonapi;

import com.exoreaction.xorcery.builders.With;
import com.exoreaction.xorcery.json.JsonElement;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author rickardoberg
 */

public record Error(ObjectNode json)
        implements JsonElement {

    public record Builder(ObjectNode builder)
            implements With<Builder>
    {
        public Builder() {
            this(JsonNodeFactory.instance.objectNode());
        }

        public Builder status(int value) {
            builder.set("status", builder.textNode(Integer.toString(value)));
            return this;
        }

        public Builder source(Source source) {
            builder.set("source", source.object());
            return this;
        }

        public Builder title(String value) {
            builder.set("title", builder.textNode(value));
            return this;
        }

        public Builder detail(String value) {
            builder.set("detail", builder.textNode(value));
            return this;
        }

        public Error build() {
            return new Error(builder);
        }
    }

    public String getStatus() {
        return getString("status").orElse(null);
    }

    public Source getSource() {
        return new Source(object().path("source") instanceof ObjectNode object ? object :
                JsonNodeFactory.instance.objectNode());
    }

    public String getTitle() {
        return getString("title").orElse("");
    }

    public String getDetail() {
        return getString("detail").orElse("");
    }
}
