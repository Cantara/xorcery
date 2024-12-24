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
package dev.xorcery.jersey.server;

import com.fasterxml.jackson.databind.JsonNode;
import dev.xorcery.configuration.Configuration;
import dev.xorcery.json.JsonElement;

import java.util.Map;
import java.util.Optional;

public record JerseyConfiguration(Configuration configuration) {
    public static JerseyConfiguration get(Configuration configuration) {
        return new JerseyConfiguration(configuration.getConfiguration("jersey.server"));
    }

    public Map<String, Object> getProperties()
    {
        return JsonElement.toFlatMap(configuration.getConfiguration("properties").json(), JsonElement::toObject);
    }

    public Optional<Map<String, String>> getMediaTypes() {
        return configuration.getObjectAs("mediaTypes", objectNode -> JsonElement.toMap(objectNode, JsonNode::textValue));
    }
}
