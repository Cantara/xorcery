package com.exoreaction.xorcery.service.dns.registration.azure.model;

import com.exoreaction.xorcery.builders.With;
import com.exoreaction.xorcery.json.model.JsonElement;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public record AzureDnsTXTRecordRequest(ObjectNode json)
        implements JsonElement {
    public record Builder(ObjectNode builder)
            implements With<AzureDnsTXTRecordRequest.Builder> {
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

        public AzureDnsTXTRecordRequest build() {
            return new AzureDnsTXTRecordRequest(builder);
        }
    }
}
