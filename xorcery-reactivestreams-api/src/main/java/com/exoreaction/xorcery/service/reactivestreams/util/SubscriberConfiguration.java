package com.exoreaction.xorcery.service.reactivestreams.util;

import com.exoreaction.xorcery.configuration.model.Configuration;

public record SubscriberConfiguration(Configuration configuration) {

    public String getAuthority() {
        return configuration.getString("authority").orElse(null);
    }

    public String getStream() {
        return configuration.getString("stream").orElseThrow(() -> new IllegalArgumentException("Missing stream name"));
    }

    public Configuration getConfiguration() {
        return configuration.getConfiguration("configuration");
    }
}
