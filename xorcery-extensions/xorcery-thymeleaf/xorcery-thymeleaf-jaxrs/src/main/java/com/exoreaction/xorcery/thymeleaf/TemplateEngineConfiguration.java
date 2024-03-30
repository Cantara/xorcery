package com.exoreaction.xorcery.thymeleaf;

import com.exoreaction.xorcery.configuration.Configuration;

public record TemplateEngineConfiguration(Configuration configuration) {

    public static TemplateEngineConfiguration get(Configuration configuration)
    {
        return new TemplateEngineConfiguration(configuration.getConfiguration("thymeleaf"));
    }


}
