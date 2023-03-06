package com.exoreaction.xorcery.service.dns.registration.azure.model;

import com.exoreaction.xorcery.builders.With;
import com.exoreaction.xorcery.json.model.JsonElement;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

public record AzureDnsARecordsRequest(ArrayNode json)
        implements JsonElement {
    public record Builder(ArrayNode builder)
            implements With<AzureDnsARecordsRequest.Builder> {
        public Builder() {
            this(JsonNodeFactory.instance.arrayNode());
        }

        public Builder aRecord(AzureDnsARecordRequest aRecord) {
            builder.add(aRecord.json());
            return this;
        }
        public AzureDnsARecordsRequest build() {
            return new AzureDnsARecordsRequest(builder);
        }
    }
}
