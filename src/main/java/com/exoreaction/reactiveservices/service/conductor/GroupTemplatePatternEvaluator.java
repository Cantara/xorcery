package com.exoreaction.reactiveservices.service.conductor;

import com.exoreaction.reactiveservices.configuration.Configuration;
import com.exoreaction.reactiveservices.jsonapi.model.Attributes;
import com.exoreaction.reactiveservices.server.model.ServiceResourceObject;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.jexl3.*;

import java.util.Optional;

public class GroupTemplatePatternEvaluator
        implements JexlContext {

    private static final JexlEngine jexl = new JexlBuilder().cache(512).strict(true).silent(false).create();

    private ServiceResourceObject serviceResourceObject;
    private String rel;

    public GroupTemplatePatternEvaluator(ServiceResourceObject serviceResourceObject, String rel) {
        this.serviceResourceObject = serviceResourceObject;
        this.rel = rel;
    }

    public boolean eval(String expression) {
        JexlContext context = new ObjectContext<>(jexl, this);

        JexlExpression expr = jexl.createExpression(expression);

        return Boolean.TRUE.equals(expr.evaluate(context));
    }

    @Override
    public Object get(String s) {
        return switch (s) {
            case "type" -> serviceResourceObject.resourceObject().getType();
            case "rel" -> rel;
            default -> Optional.of(serviceResourceObject.resourceObject().getAttributes().object()).map(o -> o.get(s)).map(v ->
                    switch (v.getNodeType()) {
                        case ARRAY, BINARY, MISSING, NULL, OBJECT, POJO -> null;
                        case BOOLEAN -> v.booleanValue();
                        case NUMBER -> v.isIntegralNumber() ? v.longValue() : v.doubleValue();
                        case STRING -> v.textValue();
                    }).orElse(null);
        };
    }

    @Override
    public void set(String s, Object o) {
        // Ignore
    }

    @Override
    public boolean has(String s) {
        return switch (s) {
            case "type","rel" -> true;
            default -> Optional.of(serviceResourceObject.resourceObject().getAttributes().object()).map(o -> o.get(s)).map(v ->
                    switch (v.getNodeType()) {
                        case ARRAY, BINARY, MISSING, NULL, OBJECT, POJO -> false;
                        case BOOLEAN,NUMBER,STRING -> true;
                    }).isPresent();
        };
    }
}
