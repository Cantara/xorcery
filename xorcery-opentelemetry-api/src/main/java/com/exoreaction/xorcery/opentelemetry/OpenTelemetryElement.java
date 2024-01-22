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
package com.exoreaction.xorcery.opentelemetry;

import com.exoreaction.xorcery.builders.WithContext;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.json.JsonElement;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Collections;
import java.util.Map;

/**
 * @author rickardoberg
 * @since 18/01/2024
 */

public interface OpenTelemetryElement
    extends WithContext<Configuration>
{
    default Map<String,String> getAttributes()
    {
        Map<String, String> attributes = context()
                .getObjectAs("attributes", JsonElement.toMap(JsonNode::asText))
                .orElse(Collections.emptyMap());
        attributes.entrySet().removeIf(entry -> entry.getValue() == null);
        return attributes;
    }
}
