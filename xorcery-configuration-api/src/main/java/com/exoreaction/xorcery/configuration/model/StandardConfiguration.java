package com.exoreaction.xorcery.configuration.model;

import com.exoreaction.xorcery.builders.WithContext;

import java.io.File;
import java.io.IOException;
import java.net.URI;

/**
 * @author rickardoberg
 * @since 20/04/2022
 */

public interface StandardConfiguration
        extends WithContext<Configuration> {
    default String getId() {
        return context().getString("id").orElse(null);
    }

    default String getHost() {
        return context().getString("host").orElse(null);
    }

    default String getEnvironment() {
        return context().getString("environment").orElse(null);
    }

    default String getTag() {
        return context().getString("tag").orElse(null);
    }

    default String getHome() {
        return context().getString("home").orElseGet(() ->
        {
            try {
                return new File(".").getCanonicalPath();
            } catch (IOException e) {
                return new File(".").getAbsolutePath();
            }
        });
    }

    default URI getServerUri() {
        return context().getURI("server.uri").orElseThrow();
    }
}
