package com.exoreaction.xorcery.opentelemetry.jvm;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.opentelemetry.OpenTelemetryElement;

/**
 * @author rickardoberg
 * @since 18/01/2024
 */

public record OpenTelemetryJvmConfiguration(Configuration context)
    implements OpenTelemetryElement
{
    public static OpenTelemetryJvmConfiguration get(Configuration configuration)
    {
        return new OpenTelemetryJvmConfiguration(configuration.getConfiguration("opentelemetry.instrumentations.jvm"));
    }
}
