package com.exoreaction.xorcery.service.dns.registration.azure.model;

import com.exoreaction.xorcery.builders.With;
import com.exoreaction.xorcery.json.model.JsonElement;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

public record AzureDnsTXTRecord(ObjectNode json)
        implements JsonElement, Iterable<String> {
    public record Builder(ObjectNode builder)
            implements With<AzureDnsTXTRecord.Builder> {
        public Builder() {
            this(JsonNodeFactory.instance.objectNode());
        }

        public Builder value(String value) {
            if (builder.path("value") instanceof ArrayNode array) {
                array.add(value);
            } else {
                var array = builder.arrayNode();
                array.add(value);
                builder.set("value", array);
            }
            return this;
        }

        public AzureDnsTXTRecord build() {
            return new AzureDnsTXTRecord(builder);
        }
    }

    public List<String> getValues() {
        if (object().path("value") instanceof ArrayNode array) {
            return JsonElement.getValuesAs(array, JsonNode::asText);
        }
        return Collections.emptyList();
    }

    @Override
    public Iterator<String> iterator() {
        return getValues().iterator();
    }

    public Stream<String> stream() {
        return getValues().stream();
    }
}
