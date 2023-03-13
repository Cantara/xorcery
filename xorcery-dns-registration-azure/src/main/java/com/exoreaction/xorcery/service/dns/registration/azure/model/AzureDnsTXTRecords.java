package com.exoreaction.xorcery.service.dns.registration.azure.model;

import com.exoreaction.xorcery.builders.With;
import com.exoreaction.xorcery.json.model.JsonElement;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

public record AzureDnsTXTRecords(ArrayNode json)
        implements JsonElement, Iterable<AzureDnsTXTRecord> {
    public record Builder(ArrayNode builder)
            implements With<AzureDnsTXTRecords.Builder> {
        public Builder() {
            this(JsonNodeFactory.instance.arrayNode());
        }

        public Builder txtRecord(AzureDnsTXTRecord txtRecord) {
            builder.add(txtRecord.json());
            return this;
        }

        public AzureDnsTXTRecords build() {
            return new AzureDnsTXTRecords(builder);
        }
    }

    public List<AzureDnsTXTRecord> getRecords() {
        return JsonElement.getValuesAs(array(), AzureDnsTXTRecord::new);
    }

    @Override
    public Iterator<AzureDnsTXTRecord> iterator() {
        return getRecords().iterator();
    }

    public Stream<AzureDnsTXTRecord> stream() {
        return getRecords().stream();
    }
}
