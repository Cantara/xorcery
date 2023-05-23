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
package com.exoreaction.xorcery.opensearch;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.configuration.ServiceConfiguration;
import com.fasterxml.jackson.databind.JsonNode;

import java.net.URI;
import java.util.List;
import java.util.Optional;

public record OpenSearchConfiguration(Configuration context)
    implements ServiceConfiguration
{
    public JsonNode getComponentTemplates() {
        return context.getJson("componentTemplates").orElseThrow(() ->
                new IllegalArgumentException("Missing opensearch.componentTemplates configuration"));
    }

    public JsonNode getIndexTemplates() {
        return context.getJson("indexTemplates").orElseThrow(() ->
                new IllegalArgumentException("Missing opensearch.indexTemplates configuration"));
    }

    public boolean isDeleteOnExit() {
        return context.getBoolean("deleteOnExit").orElse(false);
    }

    public URI getURL() {
        return context.getURI("url").orElseThrow(()->new IllegalArgumentException("Missing opensearch.url configuration"));
    }

    public Optional<List<OpenSearchService.Publisher>> getPublishers() {
        return context.getObjectListAs("publishers", OpenSearchService.Publisher::new);
    }
}
