package com.exoreaction.reactiveservices.disruptor;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.util.HashMap;
import java.util.Map;

public record Metadata(Map<String, String> metadata)
{
    public Metadata() {
        this(new HashMap<>());
    }

    @JsonAnySetter
    public void add(String name, String value)
    {
        metadata.put(name, value);
    }

    public void add(Metadata metadata)
    {
        this.metadata.putAll(metadata.getMetadata());
    }

    public void clear()
    {
        metadata.clear();
    }

    @JsonAnyGetter
    public Map<String, String> getMetadata() {
        return metadata;
    }
}
