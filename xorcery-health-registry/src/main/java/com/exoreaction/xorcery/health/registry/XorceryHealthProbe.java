package com.exoreaction.xorcery.health.registry;

import java.util.function.Supplier;

public class XorceryHealthProbe {
    final String key;
    final Supplier<Object> probe;

    public XorceryHealthProbe(String key, Supplier<Object> probe) {
        this.key = key;
        this.probe = probe;
    }

    public String getKey() {
        return key;
    }

    public Supplier<Object> getProbe() {
        return probe;
    }
}
