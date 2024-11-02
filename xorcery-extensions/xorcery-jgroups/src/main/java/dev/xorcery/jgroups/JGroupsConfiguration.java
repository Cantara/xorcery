package dev.xorcery.jgroups;

import com.fasterxml.jackson.databind.JsonNode;
import dev.xorcery.configuration.Configuration;
import dev.xorcery.configuration.ServiceConfiguration;
import dev.xorcery.json.JsonElement;
import dev.xorcery.util.Resources;

import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static dev.xorcery.json.JsonElement.toMap;

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
