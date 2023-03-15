package com.exoreaction.xorcery.service.dns.registration.azure.model;

import com.exoreaction.xorcery.builders.With;
import com.exoreaction.xorcery.json.model.JsonElement;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public record AzureDnsSRVRecord(ObjectNode json)
        implements JsonElement {
    public record Builder(ObjectNode builder)
            implements With<AzureDnsSRVRecord.Builder> {
        public Builder() {
            this(JsonNodeFactory.instance.objectNode());
        }

        public Builder priority(int priority) {
            builder.set("priority", builder.numberNode(priority));
            return this;
        }

        public Builder weight(int weight) {
            builder.set("weight", builder.numberNode(weight));
            return this;
        }

        public Builder port(int port) {
            builder.set("port", builder.numberNode(port));
            return this;
        }

        public Builder target(String target) {
            builder.put("target", target);
            return this;
        }

        public AzureDnsSRVRecord build() {
            return new AzureDnsSRVRecord(builder);
        }
    }

    public String getTarget() {
        return object().get("target").asText();
    }

    public int getPort() {
        return object().get("port").asInt();
    }
}
