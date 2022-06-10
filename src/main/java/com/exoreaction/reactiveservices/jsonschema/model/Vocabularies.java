package com.exoreaction.reactiveservices.jsonschema.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jetbrains.annotations.NotNull;

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

    @NotNull
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
