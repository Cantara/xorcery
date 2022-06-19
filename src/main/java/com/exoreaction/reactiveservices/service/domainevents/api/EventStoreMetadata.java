package com.exoreaction.reactiveservices.service.domainevents.api;

import com.exoreaction.reactiveservices.cqrs.metadata.Metadata;

public record EventStoreMetadata(Metadata metadata) {

    public record Builder(Metadata.Builder metadata) {
        public Builder streamId(String value) {
            metadata.add("streamId", value);
            return this;
        }

        public Builder revision(long value) {
            metadata.add("revision", value);
            return this;
        }

        public Builder contentType(String value) {
            metadata.add("contentType", value);
            return this;
        }

        public EventStoreMetadata build() {
            return new EventStoreMetadata(metadata.build());
        }
    }

    public long revision() {
        return metadata.getLong("revision").orElseThrow();
    }

    public String contentType() {
        return metadata.getString("contentType").orElseThrow();
    }
}
