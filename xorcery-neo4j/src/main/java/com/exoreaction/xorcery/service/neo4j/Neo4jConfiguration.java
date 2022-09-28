package com.exoreaction.xorcery.service.neo4j;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.json.JsonElement;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;

public record Neo4jConfiguration(Configuration configuration) {
    Path databasePath() {
        return new File(configuration.getString("path").orElseThrow()).toPath();
    }

    Map<String, String> settings() {
        return JsonElement.toFlatMap(configuration().getConfiguration("settings").json(), JsonNode::asText);
    }
}
