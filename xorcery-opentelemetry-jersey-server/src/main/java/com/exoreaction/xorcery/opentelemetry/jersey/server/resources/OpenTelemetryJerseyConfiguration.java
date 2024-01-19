package com.exoreaction.xorcery.opentelemetry.jersey.server.resources;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.opentelemetry.OpenTelemetryElement;

/**
 * @author rickardoberg
 * @since 18/01/2024
 */

public record OpenTelemetryJerseyConfiguration(Configuration context)
    implements OpenTelemetryElement
{
    public static OpenTelemetryJerseyConfiguration get(Configuration configuration)
    {
        return new OpenTelemetryJerseyConfiguration(configuration.getConfiguration("opentelemetry.instrumentations.jersey"));
    }
}
