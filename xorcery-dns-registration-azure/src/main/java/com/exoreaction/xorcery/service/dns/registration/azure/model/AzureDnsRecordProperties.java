package com.exoreaction.xorcery.service.dns.registration.azure.model;

import com.exoreaction.xorcery.builders.With;
import com.exoreaction.xorcery.json.model.JsonElement;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;

public record AzureDnsRecordProperties(ObjectNode json)
        implements JsonElement {
    public record Builder(ObjectNode builder)
            implements With<AzureDnsRecordProperties.Builder> {
        public Builder() {
            this(JsonNodeFactory.instance.objectNode());
        }

        public Builder metadata(AzureDnsMetadata metadata) {
            builder.set("metadata", metadata.json());
            return this;
        }

        public Builder ttl(int ttl) {
            builder.set("ttl", builder.numberNode(ttl));
            return this;
        }

        public Builder aRecords(AzureDnsARecords aRecords) {
            builder.set("aRecords", aRecords.array());
            return this;
        }

        public Builder txtRecords(AzureDnsTXTRecords txtRecords) {
            builder.set("txtRecords", txtRecords.array());
            return this;
        }

        public Builder srvRecords(AzureDnsSRVRecords srvRecords) {
            builder.set("srvRecords", srvRecords.array());
            return this;
        }

        public AzureDnsRecordProperties build() {
            return new AzureDnsRecordProperties(builder);
        }
    }

    public int getTTL() {
        return object().get("ttl").asInt();
    }

    public AzureDnsTXTRecords getTXTRecords() {
        return new AzureDnsTXTRecords(
            object().path("txtRecords") instanceof ArrayNode array ?
                array : JsonNodeFactory.instance.arrayNode());
    }

    public AzureDnsARecords getARecords() {
        return new AzureDnsARecords(
            object().path("aRecords") instanceof ArrayNode array ?
                array : JsonNodeFactory.instance.arrayNode());
    }

    public AzureDnsSRVRecords getSRVRecords() {
        return new AzureDnsSRVRecords(
            object().path("srvRecords") instanceof ArrayNode array ?
                array : JsonNodeFactory.instance.arrayNode());
    }
}
