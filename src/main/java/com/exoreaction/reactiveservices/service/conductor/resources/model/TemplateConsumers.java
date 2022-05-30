package com.exoreaction.reactiveservices.service.conductor.resources.model;

import com.exoreaction.reactiveservices.configuration.Configuration;
import com.exoreaction.reactiveservices.jsonapi.model.Link;
import com.exoreaction.reactiveservices.server.model.ServiceResourceObject;
import com.exoreaction.reactiveservices.service.conductor.GroupTemplatePatternEvaluator;
import jakarta.json.JsonObject;

import java.util.List;

public record TemplateConsumers(JsonObject json) {

    public boolean isMany() {
        return json.getBoolean("many", false);
    }

    public boolean isConsumer(ServiceResourceObject service, Configuration configuration) {
        String expression = json.getString("pattern");
        List<Link> links = service.resourceObject().getLinks().getLinks();
        if (links.isEmpty()) {
            return new GroupTemplatePatternEvaluator(configuration, service, null).eval(expression);
        } else {
            return links.stream().anyMatch(link ->
                    new GroupTemplatePatternEvaluator(configuration, service, link.rel()).eval(expression));
        }
    }
}
