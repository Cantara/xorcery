package com.exoreaction.reactiveservices.service.domainevents.api;

import com.exoreaction.reactiveservices.disruptor.Metadata;
import jakarta.json.JsonObjectBuilder;

public record EventStoreMetadata(Metadata metadata) {

    public long position() {
        return metadata.getLong("position").orElseThrow();
    }
}
