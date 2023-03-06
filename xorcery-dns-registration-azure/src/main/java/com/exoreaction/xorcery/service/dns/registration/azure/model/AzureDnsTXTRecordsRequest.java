package com.exoreaction.xorcery.service.dns.registration.azure.model;

import com.exoreaction.xorcery.builders.With;
import com.exoreaction.xorcery.json.model.JsonElement;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

public record AzureDnsTXTRecordsRequest(ArrayNode json)
        implements JsonElement {
    public record Builder(ArrayNode builder)
            implements With<AzureDnsTXTRecordsRequest.Builder> {
        public Builder() {
            this(JsonNodeFactory.instance.arrayNode());
        }

        public Builder txtRecord(AzureDnsTXTRecordRequest txtRecord) {
            builder.add(txtRecord.json());
            return this;
        }

        public AzureDnsTXTRecordsRequest build() {
            return new AzureDnsTXTRecordsRequest(builder);
        }
    }
}
