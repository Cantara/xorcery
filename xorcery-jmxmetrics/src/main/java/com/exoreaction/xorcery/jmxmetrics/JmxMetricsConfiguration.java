package com.exoreaction.xorcery.jmxmetrics;

import com.exoreaction.xorcery.configuration.ComponentConfiguration;
import com.exoreaction.xorcery.configuration.Configuration;

public record JmxMetricsConfiguration(Configuration configuration)
    implements ComponentConfiguration
{
}
