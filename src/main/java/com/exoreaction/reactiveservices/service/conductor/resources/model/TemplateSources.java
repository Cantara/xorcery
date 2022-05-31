package com.exoreaction.reactiveservices.service.conductor.resources.model;

import com.exoreaction.reactiveservices.configuration.Configuration;
import com.exoreaction.reactiveservices.server.model.ServiceResourceObject;
import com.exoreaction.reactiveservices.service.conductor.GroupTemplatePatternEvaluator;
import com.fasterxml.jackson.databind.node.ObjectNode;

public record TemplateSources(ObjectNode json) {

    public boolean isMany() {
        return json.path("many").asBoolean(false);
    }

    public boolean isSource(ServiceResourceObject service, Configuration configuration) {
        String expression = json.path("pattern").textValue();
        return service.resourceObject().getLinks().getLinks().stream().anyMatch(link ->
                new GroupTemplatePatternEvaluator(configuration, service, link.rel()).eval(expression));
    }
}
