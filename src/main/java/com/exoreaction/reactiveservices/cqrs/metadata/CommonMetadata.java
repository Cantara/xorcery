package com.exoreaction.reactiveservices.cqrs.metadata;

public interface CommonMetadata {

    interface Builder<T>
    {
        Metadata.Builder builder();

        default T timestamp(long timestamp) {
            builder().add("timestamp", timestamp);
            return (T)this;
        }
    }

    Metadata metadata();

    default long getTimestamp() {
        return metadata().getLong("timestamp").orElse(0L);
    }
}
