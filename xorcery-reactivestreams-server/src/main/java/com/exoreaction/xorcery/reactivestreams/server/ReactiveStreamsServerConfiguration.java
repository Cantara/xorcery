package com.exoreaction.xorcery.reactivestreams.server;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.configuration.ServiceConfiguration;

import java.time.Duration;

public record ReactiveStreamsServerConfiguration(Configuration context)
    implements ServiceConfiguration
{
    public Duration getIdleTimeout() {
        return Duration.parse("PT" + context.getString("idleTimeout").orElse("-1s"));
    }

    public int getMaxTextMessageSize()
    {
        return context.getInteger("maxTextMessageSize").orElse(1048576);
    }
}
