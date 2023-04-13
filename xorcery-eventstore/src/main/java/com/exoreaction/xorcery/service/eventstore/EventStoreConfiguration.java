package com.exoreaction.xorcery.service.eventstore;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.configuration.model.ServiceConfiguration;

public record EventStoreConfiguration(Configuration context)
        implements ServiceConfiguration {

    public String getURL() {
        return context.getString("url").orElseThrow(() -> new IllegalArgumentException("Missing URL"));
    }
}
