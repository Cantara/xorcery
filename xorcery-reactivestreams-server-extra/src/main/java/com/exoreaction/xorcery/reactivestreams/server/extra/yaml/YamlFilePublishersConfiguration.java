package com.exoreaction.xorcery.reactivestreams.server.extra.yaml;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.configuration.ServiceConfiguration;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.util.Collections;
import java.util.List;

public record YamlFilePublishersConfiguration(Configuration context)
        implements ServiceConfiguration {
    public List<YamlFilePublisherConfiguration> getYamlFilePublishers() {
        return context.getObjectListAs("publishers", YamlFilePublisherConfiguration::new)
                .orElse(Collections.emptyList());
    }

    public record YamlFilePublisherConfiguration(Configuration configuration) {
        public YamlFilePublisherConfiguration(ObjectNode json) {
            this(new Configuration(json));
        }

        public String getStream() {
            return configuration.getString("stream").orElseThrow(Configuration.missing("stream"));
        }

        public String getFile() {
            return configuration.getString("file").orElseThrow(Configuration.missing("file"));
        }
    }
}
