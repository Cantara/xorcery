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
package dev.xorcery.opensearch.templates;

import com.fasterxml.jackson.databind.JsonNode;
import dev.xorcery.configuration.Configuration;
import dev.xorcery.configuration.ServiceConfiguration;
import dev.xorcery.json.JsonElement;

import java.net.URI;
import java.util.Collections;
import java.util.List;

public record OpenSearchTemplatesConfiguration(Configuration context)
        implements ServiceConfiguration {

    public static OpenSearchTemplatesConfiguration get(Configuration configuration) {
        return new OpenSearchTemplatesConfiguration(configuration.getConfiguration("opensearch.templates"));
    }

    public List<Template> getComponentTemplates() {
        return context.getObjectListAs("componentTemplates", Template::new).orElse(Collections.emptyList());
    }

    public List<Template> getIndexTemplates() {
        return context.getObjectListAs("indexTemplates", Template::new).orElse(Collections.emptyList());
    }

    public record Template(JsonNode json)
        implements JsonElement
    {
        public String getId()
        {
            return getString("id").orElseThrow(Configuration.missing("id"));
        }

        public URI getResource()
        {
            return getString("resource").map(URI::create).orElseThrow(Configuration.missing("resource"));
        }
    }
}
