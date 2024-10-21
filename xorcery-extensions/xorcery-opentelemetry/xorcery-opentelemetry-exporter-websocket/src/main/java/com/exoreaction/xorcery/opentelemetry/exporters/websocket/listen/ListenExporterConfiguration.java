package com.exoreaction.xorcery.opentelemetry.exporters.websocket.listen;

import com.exoreaction.xorcery.configuration.Configuration;

import java.net.URI;

public record ListenExporterConfiguration(Configuration configuration) {
    public static ListenExporterConfiguration get(Configuration configuration) {
        return new ListenExporterConfiguration(configuration.getConfiguration("opentelemetry.exporters.websocket.listen"));
    }

    public String getPath()
    {
        return configuration.getString("path").orElseThrow(Configuration.missing("path"));
    }

    public URI getUri() {
        return configuration.getURI("uri").orElseThrow(Configuration.missing("uri"));
    }
}
