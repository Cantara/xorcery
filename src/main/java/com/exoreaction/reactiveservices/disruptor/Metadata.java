package com.exoreaction.reactiveservices.disruptor;

import com.fasterxml.jackson.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record Metadata(Map<String, String> metadata)
{
    @JsonCreator(mode= JsonCreator.Mode.DELEGATING)
    public Metadata(@JsonProperty("metadata") Map<String, String> metadata) {
        if (metadata == null)
        {
            this.metadata = new HashMap<>();
        } else {
            this.metadata = metadata;
        }
    }

    public Metadata() {
        this(new HashMap<>());
    }

    @JsonAnySetter
    public Metadata add(String name, String value)
    {
        metadata.put(name, value);
        return this;
    }

    public Metadata add(Metadata metadata)
    {
        this.metadata.putAll(metadata.getMetadata());
        return this;
    }

    public Metadata clear()
    {
        metadata.clear();
        return this;
    }

    @JsonValue
    public Map<String, String> getMetadata() {
        return metadata;
    }

    public Optional<String> get(String name)
    {
        return Optional.ofNullable(metadata.get(name));
    }
}
