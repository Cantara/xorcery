package com.exoreaction.xorcery.jmxmetrics;

import com.exoreaction.xorcery.configuration.ComponentConfiguration;
import com.exoreaction.xorcery.configuration.Configuration;

public record JmxMetricsConfiguration(Configuration configuration)
    implements ComponentConfiguration
{
    public String getURI()
    {
        return configuration.getString("uri").orElseThrow(missing("jmxmetrics.uri"));
    }

    public String getExternalURI()
    {
        return configuration.getString("externalUri").orElseThrow(missing("jmxmetrics.externalUri"));
    }

    public int getPort() {
        return configuration.getInteger("port").orElse(1099);
    }
}
