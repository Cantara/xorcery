package com.exoreaction.xorcery.service.conductor.api;

import com.fasterxml.jackson.databind.node.ObjectNode;

public record ServiceTemplate(ObjectNode json) {

    public boolean isMany() {
        return json.path("many").asBoolean(false);
    }

    public String pattern()
    {
        return json.path("pattern").textValue();
    }
}
