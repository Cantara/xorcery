package com.exoreaction.xorcery.service.neo4jprojections;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.configuration.model.ServiceConfiguration;

public record Neo4jProjectionsConfiguration(Configuration context)
    implements ServiceConfiguration
{
    public boolean isEventSubscriberEnabled() {
        return context.getBoolean("eventsubscriber.enabled").orElse(true);
    }

    public boolean isCommitPublisherEnabled() {
        return context.getBoolean("commitpublisher.enabled").orElse(true);
    }
}
