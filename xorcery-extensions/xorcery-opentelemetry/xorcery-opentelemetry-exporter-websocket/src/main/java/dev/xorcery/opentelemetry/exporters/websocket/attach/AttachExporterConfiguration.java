package dev.xorcery.opentelemetry.exporters.websocket.attach;

import dev.xorcery.configuration.Configuration;

import java.net.URI;

public record AttachExporterConfiguration(Configuration configuration) {
    public static AttachExporterConfiguration get(Configuration configuration) {
        return new AttachExporterConfiguration(configuration.getConfiguration("opentelemetry.exporters.websocket.attach"));
    }
    public URI getCollectorUri() {
        URI host =  configuration.getURI("host").orElseThrow(Configuration.missing("host"));
        if (host.getPath().equals("/"))
        {
            return host.resolve("collector/v1");
        } else
        {
            return host;
        }
    }

    public boolean isOptimizeResource()
    {
        return configuration.getBoolean("optimizeResource").orElse(false);
    }
}
