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
package dev.xorcery.opensearch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
import dev.xorcery.configuration.ComponentConfiguration;
import dev.xorcery.configuration.Configuration;
import dev.xorcery.configuration.ServiceConfiguration;
import dev.xorcery.json.JsonElement;

import java.net.URI;
import java.util.List;
import java.util.Optional;

public record OpenSearchConfiguration(Configuration context)
        implements ServiceConfiguration {
    public JsonNode getComponentTemplates() {
        return context.getJson("componentTemplates").orElseThrow(missing("opensearch.componentTemplates"));
    }

    public JsonNode getIndexTemplates() {
        return context.getJson("indexTemplates").orElseThrow(missing("opensearch.indexTemplates"));
    }

    public boolean isDeleteOnExit() {
        return context.getBoolean("deleteOnExit").orElse(false);
    }

    public URI getURI() {
        return context.getURI("uri").orElseThrow(missing("opensearch.uri"));
    }

    public Optional<List<Publisher>> getPublishers() {
        return context.getObjectListAs("publishers", Publisher::new);
    }

    public record Publisher(ContainerNode<?> json)
            implements JsonElement, ComponentConfiguration {
        Optional<URI> getURI() {
            return JsonElement.super.getURI("server.uri");
        }

        Optional<Configuration> getServerConfiguration() {
            return getObjectAs("server.configuration", Configuration::new);
        }

        Optional<Configuration> getClientConfiguration() {
            return getObjectAs("client.configuration", Configuration::new);
        }
    }
}
