package com.exoreaction.xorcery.reactivestreams.persistentsubscriber.providers;

import com.exoreaction.xorcery.configuration.Configuration;

import java.util.Optional;

public record BasePersistentSubscriberConfiguration(Configuration configuration) {
    // if true in configuration, skip until System.currentTimeMillis()
    public boolean getSkipOld() {
        return configuration.getBoolean("skipOld").orElse(false);
    }

    // if set to long timestamp, skip until this timestamp is seen in event metadata
    public Optional<Long> getSkipUntil() {
        return configuration.getLong("skipUntil");
    }
}
