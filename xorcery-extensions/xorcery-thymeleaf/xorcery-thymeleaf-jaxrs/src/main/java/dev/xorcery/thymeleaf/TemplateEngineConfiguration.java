package dev.xorcery.thymeleaf;

import dev.xorcery.configuration.Configuration;

public record TemplateEngineConfiguration(Configuration configuration) {

    public static TemplateEngineConfiguration get(Configuration configuration)
    {
        return new TemplateEngineConfiguration(configuration.getConfiguration("thymeleaf"));
    }


}
