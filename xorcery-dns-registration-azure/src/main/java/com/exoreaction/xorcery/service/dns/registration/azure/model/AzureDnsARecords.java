package com.exoreaction.xorcery.service.dns.registration.azure.model;

import com.exoreaction.xorcery.builders.With;
import com.exoreaction.xorcery.json.model.JsonElement;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

public record AzureDnsARecords(ArrayNode json)
        implements JsonElement, Iterable<AzureDnsARecord> {
    public record Builder(ArrayNode builder)
            implements With<AzureDnsARecords.Builder> {
        public Builder() {
            this(JsonNodeFactory.instance.arrayNode());
        }

        public Builder aRecord(AzureDnsARecord aRecord) {
            builder.add(aRecord.json());
            return this;
        }
        public AzureDnsARecords build() {
            return new AzureDnsARecords(builder);
        }
    }

    public List<AzureDnsARecord> getRecords() {
        return JsonElement.getValuesAs(array(), AzureDnsARecord::new);
    }

    @Override
    public Iterator<AzureDnsARecord> iterator() {
        return getRecords().iterator();
    }

    public Stream<AzureDnsARecord> stream() {
        return getRecords().stream();
    }
}
