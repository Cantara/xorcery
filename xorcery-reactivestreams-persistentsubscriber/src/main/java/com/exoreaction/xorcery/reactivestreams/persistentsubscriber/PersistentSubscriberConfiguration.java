package com.exoreaction.xorcery.reactivestreams.persistentsubscriber;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.json.JsonElement;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.util.Optional;

public record PersistentSubscriberConfiguration(ObjectNode json)
        implements JsonElement {

    public String getName() {
        return getString("name").orElseThrow(Configuration.missing("name"));
    }

    public URI getURI() {
        return getURI("uri").orElseThrow(Configuration.missing("uri"));
    }

    public String getStream() {
        return getString("stream").orElseThrow(Configuration.missing("stream"));
    }

    public Optional<String> getCheckpointProvider() {
        return getString("checkpointProvider");
    }

    public Optional<String> getErrorLogProvider() {
        return getString("errorLogProvider");
    }

    public Configuration getConfiguration() {
        return new Configuration(json).getConfiguration("configuration");
    }
}
