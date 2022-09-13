package com.exoreaction.xorcery.service.opensearch.client.search;

import com.exoreaction.xorcery.json.JsonElement;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Optional;

public record Document(ObjectNode json)
    implements JsonElement
{
    public Optional<String> id()
    {
        return getString("_id");
    }

    public long timestamp()
    {
        return json().path("_source").path("@timestamp").longValue();
    }
}
