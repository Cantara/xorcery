package com.exoreaction.reactiveservices.disruptor;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import java.util.HashMap;
import java.util.Map;

public class Metadata
{
    private Map<String, String> metadata = new HashMap<>();

    @JsonAnySetter
    public void add(String name, String value)
    {
        metadata.put(name, value);
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
