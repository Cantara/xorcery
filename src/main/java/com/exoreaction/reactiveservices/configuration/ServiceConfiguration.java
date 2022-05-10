package com.exoreaction.reactiveservices.configuration;

public record ServiceConfiguration(Configuration configuration)
{
    public boolean isEnabled()
    {
        return configuration.getBoolean("enabled").orElse(true);
    }
}
