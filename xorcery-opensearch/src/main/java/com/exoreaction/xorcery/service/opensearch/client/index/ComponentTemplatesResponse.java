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
package com.exoreaction.xorcery.service.opensearch.client.index;

import com.exoreaction.xorcery.json.model.JsonElement;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.HashMap;
import java.util.Map;

public record ComponentTemplatesResponse(ObjectNode json)
        implements JsonElement {
    public Map<String, IndexTemplate> getComponentTemplates() {
        ArrayNode componentTemplates = (ArrayNode) json.get("component_templates");
        Map<String, IndexTemplate> templates = new HashMap<>();
        for (JsonNode componentTemplate : componentTemplates) {
            templates.put(componentTemplate.get("name").textValue(),
                    new IndexTemplate((ObjectNode) componentTemplate.get("component_template")));
        }
        return templates;
    }
}
