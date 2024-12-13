package dev.xorcery.opentelemetry.exporters.opensearch.attach;

import dev.xorcery.configuration.Configuration;

import java.net.URL;

public record AttachExporterConfiguration(Configuration configuration) {
    public static AttachExporterConfiguration get(Configuration configuration) {
        return new AttachExporterConfiguration(configuration.getConfiguration("opentelemetry.exporters.opensearch.attach"));
    }

    public URL getHost() {
        return configuration.getURL("host").orElseThrow(Configuration.missing("host"));
    }
}
