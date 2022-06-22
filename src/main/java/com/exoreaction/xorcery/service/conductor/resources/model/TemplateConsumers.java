package com.exoreaction.xorcery.service.conductor.resources.model;

import com.exoreaction.xorcery.jsonapi.model.Link;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import com.exoreaction.xorcery.service.conductor.GroupTemplatePatternEvaluator;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;

public record TemplateConsumers(ObjectNode json) {

    public boolean isMany() {
        return json.path("many").asBoolean(false);
    }

    public boolean isConsumer(ServiceResourceObject service) {
        String expression = json.path("pattern").textValue();
        List<Link> links = service.resourceObject().getLinks().getLinks();
        if (links.isEmpty()) {
            return new GroupTemplatePatternEvaluator(service, null).eval(expression);
        } else {
            return links.stream().anyMatch(link ->
                    new GroupTemplatePatternEvaluator(service, link.rel()).eval(expression));
        }
    }
}
