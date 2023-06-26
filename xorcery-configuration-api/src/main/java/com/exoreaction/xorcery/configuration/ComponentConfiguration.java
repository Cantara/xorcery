package com.exoreaction.xorcery.configuration;

import java.util.function.Supplier;

public interface ComponentConfiguration {
    default Supplier<RuntimeException> missing(String name) {
        return Configuration.missing(name);
    }
}
