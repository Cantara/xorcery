package com.exoreaction.reactiveservices.service.domainevents.api;

import java.util.HashMap;
import java.util.Map;

public class Metadata
{
    private Map<String, String> metadata = new HashMap<>();

    public void add(String name, String value)
    {
        metadata.put(name, value);
    }

    public void clear()
    {
        metadata.clear();
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }
}
