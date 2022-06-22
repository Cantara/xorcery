package com.exoreaction.xorcery.service.conductor.resources.model;

import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import com.exoreaction.xorcery.service.conductor.GroupTemplatePatternEvaluator;
import com.fasterxml.jackson.databind.node.ObjectNode;

public record TemplateSources(ObjectNode json) {

    public boolean isMany() {
        return json.path("many").asBoolean(false);
    }

    public boolean isSource(ServiceResourceObject service) {
        String expression = json.path("pattern").textValue();
        return service.resourceObject().getLinks().getLinks().stream().anyMatch(link ->
                new GroupTemplatePatternEvaluator(service, link.rel()).eval(expression));
    }
}
