package com.exoreaction.xorcery.service.dns.registration.azure.model;

import com.exoreaction.xorcery.builders.With;
import com.exoreaction.xorcery.json.model.JsonElement;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public record AzureDnsARecordRequest(ObjectNode json)
        implements JsonElement {
    public record Builder(ObjectNode builder)
            implements With<AzureDnsARecordRequest.Builder> {
        public Builder() {
            this(JsonNodeFactory.instance.objectNode());
        }

        public Builder setIP(String ipv4) {
            builder.put("ipv4Address", ipv4);
            return this;
        }

        public AzureDnsARecordRequest build() {
            return new AzureDnsARecordRequest(builder);
        }
    }
}
