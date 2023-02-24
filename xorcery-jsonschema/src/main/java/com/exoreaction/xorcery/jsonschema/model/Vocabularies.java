package com.exoreaction.xorcery.jsonschema.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

public final class Vocabularies
        implements Iterable<Map.Entry<String, Boolean>> {
    private final ObjectNode json;

    public Vocabularies(ObjectNode json) {
        this.json = json;
    }

    public static final class Builder {
        private final ObjectNode builder;

        public Builder(ObjectNode builder) {
            this.builder = builder;
        }

        public Builder() {
            this(JsonNodeFactory.instance.objectNode());
        }

        Builder vocabulary(String url, boolean value) {
            builder.set(url, builder.booleanNode(value));
            return this;
        }

        public Vocabularies build() {
            return new Vocabularies(builder);
        }

        public ObjectNode builder() {
            return builder;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (Builder) obj;
            return Objects.equals(this.builder, that.builder);
        }

        @Override
        public int hashCode() {
            return Objects.hash(builder);
        }

        @Override
        public String toString() {
            return "Builder[" +
                   "builder=" + builder + ']';
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

    public ObjectNode json() {
        return json;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Vocabularies) obj;
        return Objects.equals(this.json, that.json);
    }

    @Override
    public int hashCode() {
        return Objects.hash(json);
    }

    @Override
    public String toString() {
        return "Vocabularies[" +
               "json=" + json + ']';
    }

}
