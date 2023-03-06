package com.exoreaction.xorcery.service.dns.registration.azure.model;

import com.exoreaction.xorcery.builders.With;
import com.exoreaction.xorcery.json.model.JsonElement;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

public record AzureDnsSRVRecordsRequest(ArrayNode json)
        implements JsonElement {
    public record Builder(ArrayNode builder)
            implements With<AzureDnsSRVRecordsRequest.Builder> {
        public Builder() {
            this(JsonNodeFactory.instance.arrayNode());
        }

        public Builder srvRecord(AzureDnsSRVRecordRequest srvRecord) {
            builder.add(srvRecord.json());
            return this;
        }

        public AzureDnsSRVRecordsRequest build() {
            return new AzureDnsSRVRecordsRequest(builder);
        }
    }
}
