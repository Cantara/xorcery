package com.exoreaction.xorcery.service.opensearch.client.index;

import com.exoreaction.xorcery.json.JsonElement;
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
