package com.exoreaction.xorcery.service.log4jsubscriber;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.configuration.model.ServiceConfiguration;

public record Log4jSubscriberConfiguration(Configuration context)
    implements ServiceConfiguration
{
    public String getSubscriberStream() {
        return context.getString("subscriber.stream").orElse("logs");
    }
}
