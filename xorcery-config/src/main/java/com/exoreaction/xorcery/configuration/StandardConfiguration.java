package com.exoreaction.xorcery.configuration;

import java.io.File;
import java.io.IOException;
import java.net.URI;

/**
 * @author rickardoberg
 * @since 20/04/2022
 */

public interface StandardConfiguration {
    record Impl(Configuration configuration)
            implements StandardConfiguration {
    }

    Configuration configuration();

    default String getId() {
        return configuration().getString("id").orElse(null);
    }

    default String getHost() {
        return configuration().getString("host").orElse(null);
    }

    default String getEnvironment() {
        return configuration().getString("environment").orElse(null);
    }

    default String getTag() {
        return configuration().getString("tag").orElse(null);
    }

    default String getHome() {
        return configuration().getString("home").orElseGet(() ->
        {
            try {
                return new File(".").getCanonicalPath();
            } catch (IOException e) {
                return new File(".").getAbsolutePath();
            }
        });
    }

    default URI getServerUri() {
        return configuration().getURI("server.uri").orElseThrow();
    }

    /*
    default jakarta.ws.rs.core.UriBuilder getServerUriBuilder() {
        return jakarta.ws.rs.core.UriBuilder.fromUri(getServerUri());
    }
     */
}
