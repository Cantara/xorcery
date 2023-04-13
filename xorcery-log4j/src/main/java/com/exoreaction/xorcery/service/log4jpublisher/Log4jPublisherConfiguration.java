package com.exoreaction.xorcery.service.log4jpublisher;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.configuration.model.ServiceConfiguration;

public record Log4jPublisherConfiguration(Configuration context)
    implements ServiceConfiguration
{

    public String getAppenderName() {
        return context.getString("appender").orElse("Log4jPublisher");
    }

    public String getSubscriberAuthority() {
        return context.getString("subscriber.authority").orElse(null);
    }

    public String getSubscriberStream() {
        return context.getString("subscriber.stream").orElseThrow();
    }

    public Configuration getSubscriberConfiguration() {
        return context.getConfiguration("subscriber.configuration");
    }

    public Configuration getPublisherConfiguration() {
        return context.getConfiguration("publisher.configuration");
    }
}
