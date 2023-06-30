package com.exoreaction.xorcery.reactivestreams.server;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.configuration.ServiceConfiguration;

import java.net.URI;
import java.time.Duration;

import static com.exoreaction.xorcery.configuration.Configuration.missing;

public record ReactiveStreamsServerConfiguration(Configuration context)
        implements ServiceConfiguration {

    public static ReactiveStreamsServerConfiguration get(Configuration configuration)
    {
        return new ReactiveStreamsServerConfiguration(configuration.getConfiguration("reactivestreams.server"));
    }

    public URI getURI() {
        return context.getURI("uri").orElseThrow(missing("reactivestreams.server.uri"));
    }

    public Duration getIdleTimeout() {
        return Duration.parse("PT" + context.getString("idleTimeout").orElse("-1s"));
    }

    public int getMaxTextMessageSize() {
        return context.getInteger("maxTextMessageSize").orElse(1048576);
    }
}
