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
