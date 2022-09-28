package com.exoreaction.xorcery.service.opensearch.client.index;

import com.exoreaction.xorcery.json.JsonElement;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.HashMap;
import java.util.Map;

public record IndexTemplatesResponse(ObjectNode json)
    implements JsonElement
{
    public Map<String, IndexTemplate> getIndexTemplates() {
        ArrayNode componentTemplates = (ArrayNode) json.get("index_templates");
        Map<String, IndexTemplate> templates = new HashMap<>();
        for (JsonNode componentTemplate : componentTemplates) {
            templates.put(componentTemplate.get("name").textValue(),
                    new IndexTemplate((ObjectNode) componentTemplate.get("index_template")));
        }
        return templates;
    }
}
