package dev.xorcery.opentelemetry.collectors.websocket.listen;

import dev.xorcery.configuration.Configuration;

public record ListenCollectorConfiguration(Configuration configuration) {
    public static ListenCollectorConfiguration get(Configuration configuration) {
        return new ListenCollectorConfiguration(configuration.getConfiguration("opentelemetry.collectors.websocket.listen"));
    }

    public String getPath()
    {
        return configuration.getString("path").orElseThrow(Configuration.missing("path"));
    }
}
