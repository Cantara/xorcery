package com.exoreaction.reactiveservices.service.conductor.resources.model;

import com.exoreaction.reactiveservices.configuration.Configuration;
import com.exoreaction.reactiveservices.server.model.ServiceResourceObject;
import com.exoreaction.reactiveservices.service.conductor.GroupTemplatePatternEvaluator;
import jakarta.json.JsonObject;

public record TemplateSources(JsonObject json) {

    public boolean isMany() {
        return json.getBoolean("many", false);
    }

    public boolean isSource(ServiceResourceObject service, Configuration configuration) {
        String expression = json.getString("pattern");
        return service.resourceObject().getLinks().getLinks().stream().anyMatch(link ->
                new GroupTemplatePatternEvaluator(configuration, service, link.rel()).eval(expression));
    }
}
