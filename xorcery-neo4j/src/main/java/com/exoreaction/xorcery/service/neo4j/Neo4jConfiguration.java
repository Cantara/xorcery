/*
 * Copyright © 2022 eXOReaction AS (rickard@exoreaction.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
