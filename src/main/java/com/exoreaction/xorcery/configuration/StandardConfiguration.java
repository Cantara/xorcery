package com.exoreaction.xorcery.configuration;

import java.io.File;
import java.io.IOException;

/**
 * @author rickardoberg
 * @since 20/04/2022
 */

public interface StandardConfiguration {
    record Impl(Configuration configuration)
            implements StandardConfiguration {
    }

    Configuration configuration();

    default String host() {
        return configuration().getString("host").orElse(null);
    }

    default String environment() {
        return configuration().getString("environment").orElse(null);
    }

    default String home() {
        return configuration().getString("home").orElseGet(() ->
        {
            try {
                return new File(".").getCanonicalPath();
            } catch (IOException e) {
                return new File(".").getAbsolutePath();
            }
        });
    }
}
