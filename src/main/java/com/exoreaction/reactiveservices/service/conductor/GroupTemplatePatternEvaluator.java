package com.exoreaction.reactiveservices.service.conductor;

import com.exoreaction.reactiveservices.configuration.Configuration;
import com.exoreaction.reactiveservices.server.model.ServiceResourceObject;
import org.apache.commons.jexl3.*;

public class GroupTemplatePatternEvaluator {

    private static final JexlEngine jexl = new JexlBuilder().cache(512).strict(true).silent(false).create();

    private final Configuration configuration;
    private ServiceResourceObject serviceResourceObject;
    private String rel;

    public GroupTemplatePatternEvaluator(Configuration configuration, ServiceResourceObject serviceResourceObject, String rel) {
        this.configuration = configuration;
        this.serviceResourceObject = serviceResourceObject;
        this.rel = rel;
    }

    public boolean eval(String expression)
    {
        JexlContext context = new ObjectContext<>(jexl, this);

        JexlExpression expr = jexl.createExpression(expression);

        return Boolean.TRUE.equals(expr.evaluate(context));
    }

    public String getType()
    {
        return serviceResourceObject.resourceObject().getType();
    }

    public String getRel()
    {
        return rel;
    }

    public Configuration getConfiguration()
    {
        return configuration;
    }

    public String getEnvironment()
    {
        return configuration.getString("environment").orElse(null);
    }
}
