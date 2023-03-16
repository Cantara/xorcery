package com.exoreaction.xorcery.domainevents.publisher;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.configuration.model.ServiceConfiguration;
import com.exoreaction.xorcery.service.reactivestreams.util.SubscriberConfiguration;

public record DomainEventsConfiguration(Configuration context)
        implements ServiceConfiguration {
    public SubscriberConfiguration getSubscriberConfiguration() {
        return new SubscriberConfiguration(context.getConfiguration("subscriber"));
    }

    public Configuration getPublisherConfiguration() {
        return context.getConfiguration("publisher");
    }
}
