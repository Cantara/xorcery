package com.exoreaction.xorcery.service.dns.registration.azure.model;

import com.exoreaction.xorcery.builders.With;
import com.exoreaction.xorcery.json.model.JsonElement;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Map;

public record AzureDnsRequestMetadata(ObjectNode json)
        implements JsonElement {
    public record Builder(ObjectNode builder)
            implements With<AzureDnsRequestMetadata.Builder> {
        public Builder() {
            this(JsonNodeFactory.instance.objectNode());
        }

        public Builder metadata(String key, String value) {
            builder.put(key, value);
            return this;
        }

        public AzureDnsRequestMetadata build() {
            return new AzureDnsRequestMetadata(builder);
        }
    }

    public Map<String, String> getMetadata() {
        return JsonElement.toMap(object(), JsonNode::textValue);
    }
}
