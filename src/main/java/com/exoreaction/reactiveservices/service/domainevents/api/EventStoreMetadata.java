package com.exoreaction.reactiveservices.service.domainevents.api;

import com.exoreaction.reactiveservices.cqrs.metadata.Metadata;

public record EventStoreMetadata(Metadata metadata) {

    public long position() {
        return metadata.getLong("position").orElseThrow();
    }
}
