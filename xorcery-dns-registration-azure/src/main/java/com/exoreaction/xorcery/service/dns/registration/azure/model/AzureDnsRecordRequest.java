package com.exoreaction.xorcery.service.dns.registration.azure.model;

import com.exoreaction.xorcery.builders.With;
import com.exoreaction.xorcery.json.model.JsonElement;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public record AzureDnsRecordRequest(ObjectNode json)
        implements JsonElement {
    public record Builder(ObjectNode builder)
            implements With<AzureDnsRecordRequest.Builder> {
        public Builder() {
            this(JsonNodeFactory.instance.objectNode());
        }

        public Builder properties(AzureDnsRecordRequestProperties properties) {
            builder.set("properties", properties.json());
            return this;
        }

        public AzureDnsRecordRequest build() {
            return new AzureDnsRecordRequest(builder);
        }
    }
}
