package com.exoreaction.reactiveservices.disruptor;

import com.exoreaction.reactiveservices.configuration.Configuration;

public record DeploymentMetadata(Metadata metadata) {

    public DeploymentMetadata(Configuration configuration) {
        this(new Metadata.Builder()
                .add("environment", configuration.getString("environment").orElse("default"))
                .add("tag", configuration.getString("tag").orElse("default"))
                .add("version", configuration.getString("version").orElse("0.1.0"))
                .build());
    }

    public String environment()
    {
        return metadata.getString("environment").orElse("default");
    }

    public String tag()
    {
        return metadata.getString("tag").orElse("default");
    }

    public String version()
    {
        return metadata.getString("version").orElse("0.1.0");
    }
}
