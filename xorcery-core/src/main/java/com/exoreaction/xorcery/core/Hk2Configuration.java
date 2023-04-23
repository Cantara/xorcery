package com.exoreaction.xorcery.core;

import com.exoreaction.xorcery.configuration.model.Configuration;
import org.glassfish.hk2.runlevel.RunLevelController;

public record Hk2Configuration(Configuration configuration) {

    public RunLevelController.ThreadingPolicy getThreadingPolicy() {
        return RunLevelController.ThreadingPolicy.valueOf(configuration.getString("threadPolicy").orElse("FULLY_THREADED"));
    }

    public int getMaximumUseableThreads() {
        return configuration.getInteger("threadCount").orElse(5);
    }

    public int getRunLevel() {
        return configuration.getInteger("runLevel").orElse(20);
    }
}
