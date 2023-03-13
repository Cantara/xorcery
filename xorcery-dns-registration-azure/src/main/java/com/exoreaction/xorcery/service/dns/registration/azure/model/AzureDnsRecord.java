package com.exoreaction.xorcery.service.dns.registration.azure.model;

import com.exoreaction.xorcery.builders.With;
import com.exoreaction.xorcery.json.model.JsonElement;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public record AzureDnsRecord(ObjectNode json)
        implements JsonElement {
    public record Builder(ObjectNode builder)
            implements With<AzureDnsRecord.Builder> {
        public Builder() {
            this(JsonNodeFactory.instance.objectNode());
        }

        public Builder etag(String etag) {
            builder.put("etag", etag);
            return this;
        }

        public Builder properties(AzureDnsRecordProperties properties) {
            builder.set("properties", properties.json());
            return this;
        }

        public AzureDnsRecord build() {
            return new AzureDnsRecord(builder);
        }
    }

    public String getEtag() {
        return object().get("etag").asText();
    }

    public String getName() {
        return object().get("name").asText();
    }

    public String getType() {
        return object().get("type").asText();
    }

    public AzureDnsRecordProperties getProperties() {
        return new AzureDnsRecordProperties((ObjectNode) object().path("properties"));
    }
}
