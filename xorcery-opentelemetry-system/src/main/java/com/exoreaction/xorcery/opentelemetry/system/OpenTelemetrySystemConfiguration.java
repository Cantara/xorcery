package com.exoreaction.xorcery.opentelemetry.system;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.opentelemetry.OpenTelemetryElement;

public record OpenTelemetrySystemConfiguration(Configuration context)
        implements OpenTelemetryElement {
    public static OpenTelemetrySystemConfiguration get(Configuration configuration) {
        return new OpenTelemetrySystemConfiguration(configuration.getConfiguration("opentelemetry.instrumentations.system"));
    }
}
