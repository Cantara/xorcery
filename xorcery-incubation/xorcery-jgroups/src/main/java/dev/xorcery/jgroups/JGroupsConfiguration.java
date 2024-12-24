/*
 * Copyright © 2022 eXOReaction AS (rickard@exoreaction.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
