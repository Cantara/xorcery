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
package dev.xorcery.opentelemetry.jmx;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.xorcery.configuration.Configuration;
import dev.xorcery.json.JsonElement;
import dev.xorcery.lang.Classes;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

public record OpenTelemetryJMXConfiguration(Configuration context) {

    public static OpenTelemetryJMXConfiguration get(Configuration configuration) {
        return new OpenTelemetryJMXConfiguration(configuration.getConfiguration("opentelemetry.instrumentations.jmx"));
    }

    public Map<String, JmxAttributeConfiguration> getAttributes() {
        return context.getObjectAs("attributes", on ->
                JsonElement.toMap(on, Classes.<JsonNode, ObjectNode>cast(ObjectNode.class).andThen(JmxAttributeConfiguration::new))).orElse(Collections.emptyMap());
    }

    public record JmxAttributeConfiguration(ContainerNode<?> json)
            implements JsonElement {

        public String getObjectName()
        {
            return getString("objectName").orElseThrow(Configuration.missing("objectName"));
        }

        public String getAttributeName()
        {
            return getString("attribute").orElseThrow(Configuration.missing("attribute"));
        }

        public String getUnit()
        {
            return getString("unit").orElse("");
        }

        public String getType()
        {
            return getString("type").orElse("Gauge");
        }

        public Optional<String> getDescription() {
            return getString("description");
        }
    }
}
