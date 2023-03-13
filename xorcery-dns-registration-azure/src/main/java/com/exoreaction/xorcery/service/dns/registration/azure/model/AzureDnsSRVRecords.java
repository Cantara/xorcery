package com.exoreaction.xorcery.service.dns.registration.azure.model;

import com.exoreaction.xorcery.builders.With;
import com.exoreaction.xorcery.json.model.JsonElement;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

public record AzureDnsSRVRecords(ArrayNode json)
        implements JsonElement, Iterable<AzureDnsSRVRecord> {
    public record Builder(ArrayNode builder)
            implements With<AzureDnsSRVRecords.Builder> {
        public Builder() {
            this(JsonNodeFactory.instance.arrayNode());
        }

        public Builder srvRecord(AzureDnsSRVRecord srvRecord) {
            builder.add(srvRecord.json());
            return this;
        }

        public AzureDnsSRVRecords build() {
            return new AzureDnsSRVRecords(builder);
        }
    }

    public List<AzureDnsSRVRecord> getRecords() {
        return JsonElement.getValuesAs(array(), AzureDnsSRVRecord::new);
    }

    @Override
    public Iterator<AzureDnsSRVRecord> iterator() {
        return getRecords().iterator();
    }

    public Stream<AzureDnsSRVRecord> stream() {
        return getRecords().stream();
    }
}
