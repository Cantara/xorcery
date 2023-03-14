package com.exoreaction.xorcery.disruptor;

import com.exoreaction.xorcery.configuration.model.Configuration;

public record DisruptorConfiguration(Configuration configuration) {
    public int getSize() {
        return configuration.getInteger("size").orElse(256);
    }

    public int getShutdownTimeout() {
        return configuration.getInteger("shutdownTimeout").orElse(60);
    }
}
