package com.exoreaction.xorcery.service.neo4j;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.configuration.model.ServiceConfiguration;
import com.exoreaction.xorcery.json.model.JsonElement;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;

public record Neo4jConfiguration(Configuration context)
    implements ServiceConfiguration
{
    Path databasePath() {
        return new File(context.getString("path").orElseThrow()).toPath();
    }

    Map<String, String> settings() {
        return JsonElement.toFlatMap(context().getConfiguration("settings").json(), JsonNode::asText);
    }
}
