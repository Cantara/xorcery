package com.exoreaction.xorcery.service.requestlogpublisher;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.configuration.model.ServiceConfiguration;

public record RequestLogConfiguration(Configuration context)
        implements ServiceConfiguration {

    public String getSubscriberAuthority() {
        return context.getString("subscriber.authority").orElse(null);
    }

    public String getSubscriberStream() {
        return context.getString("subscriber.stream").orElseThrow(()->new IllegalArgumentException("Missing request log subscriber stream"));
    }

    public Configuration getSubscriberConfiguration() {
        return context.getConfiguration("subscriber.configuration");
    }

    public Configuration getPublisherConfiguration() {
        return context.getConfiguration("publisher.configuration");
    }
}
