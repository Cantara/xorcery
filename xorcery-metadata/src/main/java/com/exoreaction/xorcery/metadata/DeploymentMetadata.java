package com.exoreaction.xorcery.metadata;

import com.exoreaction.xorcery.builders.WithContext;
import com.exoreaction.xorcery.configuration.Configuration;

public interface DeploymentMetadata
        extends WithContext<Metadata> {

    interface Builder<T> {
        Metadata.Builder builder();

        default T configuration(Configuration configuration) {
            builder().add("environment", configuration.getString("environment").orElse("default"))
                    .add("tag", configuration.getString("tag").orElse("default"))
                    .add("version", configuration.getString("version").orElse("0.1.0"))
                    .add("name", configuration.getString("name").orElse("noname"))
                    .add("host", configuration.getString("host").orElse("localhost"))
                    .build();
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
