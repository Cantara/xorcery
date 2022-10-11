package com.exoreaction.xorcery.metadata;

import com.exoreaction.xorcery.builders.WithContext;

public interface CommonMetadata
    extends WithContext<Metadata>
{

    interface Builder<T>
    {
        Metadata.Builder builder();

        default T timestamp(long timestamp) {
            builder().add("timestamp", timestamp);
            return (T)this;
        }
    }

    default long getTimestamp() {
        return context().getLong("timestamp").orElse(0L);
    }
}
