package com.exoreaction.xorcery.configuration;

public record ServiceConfiguration(Configuration configuration)
{
    public boolean isEnabled()
    {
        return configuration.getBoolean("enabled").orElse(false);
    }
}
