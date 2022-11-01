package com.exoreaction.xorcery.metadata;

import com.exoreaction.xorcery.builders.WithContext;

import java.util.Optional;

public interface CommonMetadata
    extends WithContext<Metadata>
{

    interface Builder<T>
    {
        Metadata.Builder builder();

        default T timestamp(long value) {
            builder().add("timestamp", value);
            return (T)this;
        }

        default T contentType(String value) {
            builder().add("contentType", value);
            return (T)this;
        }
    }

    default long getTimestamp() {
        return context().getLong("timestamp").orElse(0L);
    }

    default Optional<String> getContentType()
    {
        return context().getString("contentType");
    }
}
