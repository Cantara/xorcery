package com.exoreaction.xorcery.service.dns.registration.azure.model;

import com.exoreaction.xorcery.builders.With;
import com.exoreaction.xorcery.json.model.JsonElement;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public record AzureDnsRecordRequestProperties(ObjectNode json)
        implements JsonElement {
    public record Builder(ObjectNode builder)
            implements With<AzureDnsRecordRequestProperties.Builder> {
        public Builder() {
            this(JsonNodeFactory.instance.objectNode());
        }

        public Builder metadata(AzureDnsRequestMetadata metadata) {
            builder.set("metadata", metadata.json());
            return this;
        }

        public Builder ttl(int ttl) {
            builder.set("ttl", builder.numberNode(ttl));
            return this;
        }

        public Builder aRecords(AzureDnsARecordsRequest aRecords) {
            builder.set("aRecords", aRecords.array());
            return this;
        }

        public Builder txtRecords(AzureDnsTXTRecordsRequest txtRecords) {
            builder.set("txtRecords", txtRecords.array());
            return this;
        }

        public Builder srvRecords(AzureDnsSRVRecordsRequest srvRecords) {
            builder.set("srvRecords", srvRecords.array());
            return this;
        }

        public AzureDnsRecordRequestProperties build() {
            return new AzureDnsRecordRequestProperties(builder);
        }
    }
}
