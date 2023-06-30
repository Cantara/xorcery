package com.exoreaction.xorcery.reactivestreams.client;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.configuration.ServiceConfiguration;

import java.time.Duration;

public record ReactiveStreamsClientConfiguration(Configuration context)
        implements ServiceConfiguration {
    public Duration getConnectTimeout() {
        return Duration.parse("PT" + context.getString("connectTimeout").orElse("5s"));
    }

    public Duration getIdleTimeout() {
        return Duration.parse("PT" + context.getString("idleTimeout").orElse("-1s"));
    }

    public boolean isAutoFragment() {
        return context.getBoolean("autoFragment").orElse(true);
    }

    public int getMaxTextMessageSize() {
        return context.getInteger("maxTextMessageSize").orElse(1048576);
    }

    public int getMaxBinaryMessageSize() {
        return context.getInteger("maxBinaryMessageSize").orElse(1048576);
    }

    public int getMaxFrameSize() {
        return context.getInteger("maxFrameSize").orElse(65536);
    }

    public int getInputBufferSize() {
        return context.getInteger("inputBufferSize").orElse(4096);
    }

    public int getOutputBufferSize() {
        return context.getInteger("maxTextMessageSize").orElse(4096);
    }
}
