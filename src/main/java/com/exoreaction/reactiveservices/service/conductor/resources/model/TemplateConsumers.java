package com.exoreaction.reactiveservices.service.conductor.resources.model;

import com.exoreaction.reactiveservices.configuration.Configuration;
import com.exoreaction.reactiveservices.jsonapi.model.Link;
import com.exoreaction.reactiveservices.server.model.ServiceResourceObject;
import com.exoreaction.reactiveservices.service.conductor.GroupTemplatePatternEvaluator;
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
