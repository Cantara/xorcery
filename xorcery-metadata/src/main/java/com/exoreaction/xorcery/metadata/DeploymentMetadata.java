package com.exoreaction.xorcery.metadata;

import com.exoreaction.xorcery.builders.WithContext;
import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.configuration.model.InstanceConfiguration;

public interface DeploymentMetadata
        extends WithContext<Metadata> {

    interface Builder<T> {
        Metadata.Builder builder();

        default T configuration(InstanceConfiguration configuration) {
            builder().add("environment", configuration.getEnvironment())
                    .add("tag", configuration.getTag())
                    .add("name", configuration.getName())
                    .add("host", configuration.getHost());
            return (T) this;
        }

        default T version(String value)
        {
            builder().add("version", value);
            return (T) this;
        }
    }

    default String getEnvironment() {
        return context().getString("environment").orElse("default");
    }

    default String getTag() {
        return context().getString("tag").orElse("default");
    }

    default String getVersion() {
        return context().getString("version").orElse("0.1.0");
    }

    default String getName() {
        return context().getString("name").orElse("noname");
    }

    default String getHost() {
        return context().getString("host").orElse("localhost");
    }
}
