package com.exoreaction.xorcery.service.eventstore.streams;

import com.exoreaction.xorcery.configuration.model.Configuration;

public record StreamsConfiguration(Configuration context) {

    public boolean isPublisherEnabled() {
        return context.getBoolean("publisher.enabled").orElse(true);
    }

    public boolean isSubscriberEnabled() {
        return context.getBoolean("subscriber.enabled").orElse(true);
    }
}
