package com.exoreaction.reactiveservices.cqrs.metadata;

import com.exoreaction.reactiveservices.configuration.Configuration;

public interface DeploymentMetadata {

    interface Builder<T> {
        Metadata.Builder builder();

        default T configuration(Configuration configuration) {
            builder().add("environment", configuration.getString("environment").orElse("default"))
                    .add("tag", configuration.getString("tag").orElse("default"))
                    .add("version", configuration.getString("version").orElse("0.1.0"))
                    .add("name", configuration.getString("name").orElse("noname"))
                    .add("host", configuration.getString("host").orElse("localhost"))
                    .build();
            return (T)this;
        }
    }

    Metadata metadata();

    default String getEnvironment()
    {
        return metadata().getString("environment").orElse("default");
    }

    default String getTag()
    {
        return metadata().getString("tag").orElse("default");
    }

    default String getVersion()
    {
        return metadata().getString("version").orElse("0.1.0");
    }

    default String getName()
    {
        return metadata().getString("name").orElse("noname");
    }
    default String getHost()
    {
        return metadata().getString("host").orElse("localhost");
    }
}
