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
package com.exoreaction.xorcery.jsonschema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Iterator;
import java.util.Map;

public record Vocabularies(ObjectNode json)
    implements Iterable<Map.Entry<String, Boolean>>
{

    public record Builder(ObjectNode builder) {
        public Builder() {
            this(JsonNodeFactory.instance.objectNode());
        }

        Builder vocabulary(String url, boolean value)
        {
            builder.set(url, builder.booleanNode(value));
            return this;
        }

        public Vocabularies build()
        {
            return new Vocabularies(builder);
        }
    }

    @Override
    public Iterator<Map.Entry<String, Boolean>> iterator() {
        Iterator<Map.Entry<String, JsonNode>> iterator = json.fields();
        return new Iterator<Map.Entry<String, Boolean>>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public Map.Entry<String, Boolean> next() {
                Map.Entry<String, JsonNode> next = iterator.next();
                return new Map.Entry<String, Boolean>() {
                    @Override
                    public String getKey() {
                        return next.getKey();
                    }

                    @Override
                    public Boolean getValue() {
                        return next.getValue().booleanValue();
                    }

                    @Override
                    public Boolean setValue(Boolean value) {
                        return next.setValue(json.booleanNode(value)).booleanValue();
                    }
                };
            }
        };
    }
}
