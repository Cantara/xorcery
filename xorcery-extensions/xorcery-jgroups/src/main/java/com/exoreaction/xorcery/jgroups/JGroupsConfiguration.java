package com.exoreaction.xorcery.jgroups;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.configuration.ServiceConfiguration;
import com.exoreaction.xorcery.json.JsonElement;
import com.exoreaction.xorcery.util.Resources;
import com.fasterxml.jackson.databind.JsonNode;

import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static com.exoreaction.xorcery.json.JsonElement.toMap;

public record JGroupsConfiguration(Configuration context)
        implements ServiceConfiguration {
    public static JGroupsConfiguration get(Configuration configuration) {
        return new JGroupsConfiguration(configuration.getConfiguration("jgroups"));
    }

    public Map<String, ChannelConfiguration> getChannels() {
        return context.getObjectAs("channels", toMap(ChannelConfiguration::new)).orElse(Collections.emptyMap());
    }

    public record ChannelConfiguration(JsonNode json)
            implements JsonElement {
        public Optional<URL> getXMLConfig()
        {
            return getString("config").flatMap(Resources::getResource);
        }
    }
}
