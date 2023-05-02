package com.exoreaction.xorcery.service.neo4j;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.configuration.model.ServiceConfiguration;
import com.exoreaction.xorcery.json.model.JsonElement;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record Neo4jConfiguration(Configuration context)
    implements ServiceConfiguration
{
    Path getDatabasePath() {
        return new File(context.getString("path").orElseThrow()).toPath();
    }

    Map<String, String> settings() {
        return JsonElement.toFlatMap(context().getConfiguration("settings").json(), JsonNode::asText);
    }

    public String getTemporarySettingsFile() {
        return context.getString("temporarySettingsFile").orElseThrow();
    }

    public boolean isWipeOnBreakingChanges() {
        return context.getBoolean("domain.wipe_on_breaking_change").orElseThrow();
    }

    public SemanticVersion getVersion() {
        return context.getString("domain.version")
                .map(SemanticVersion::from)
                .orElseThrow();
    }

    public Optional<List<DatabaseConfiguration>> getDatabases() {
        return context.getListAs("databases", json -> new DatabaseConfiguration((ObjectNode) json));
    }
}
