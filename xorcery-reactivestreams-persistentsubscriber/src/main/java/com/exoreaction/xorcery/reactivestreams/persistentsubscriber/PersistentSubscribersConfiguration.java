package com.exoreaction.xorcery.reactivestreams.persistentsubscriber;

import com.exoreaction.xorcery.configuration.Configuration;

import java.util.Collections;
import java.util.List;

public record PersistentSubscribersConfiguration(Configuration configuration) {


    public String getDefaultCheckpointProvider() {
        return configuration.getString("defaultCheckpointProvider").orElse("file");
    }

    public String getDefaultErrorLogProvider() {
        return configuration.getString("defaultErrorLogProvider").orElse("file");
    }

    public List<PersistentSubscriberConfiguration> getPersistentSubscribers() {
        return configuration.getObjectListAs("subscribers", PersistentSubscriberConfiguration::new)
                .orElse(Collections.emptyList());
    }


}
