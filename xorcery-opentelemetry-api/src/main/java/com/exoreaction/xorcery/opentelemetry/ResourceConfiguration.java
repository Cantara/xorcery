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

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.json.JsonElement;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.semconv.ResourceAttributes;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public record ResourceConfiguration(Configuration configuration) {

    public static ResourceConfiguration get(Configuration configuration)
    {
        return new ResourceConfiguration(configuration.getConfiguration("opentelemetry.resource"));
    }

    public Map<AttributeKey<Object>, Object> getResource() {
        return Optional.of(configuration.json())
                .map(ObjectNode.class::cast)
                .map(JsonElement::toMap)
                .map(map -> map.entrySet().stream().map(this::updateEntry).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
                .orElse(Collections.<AttributeKey<Object>, Object>emptyMap());
    }

    private Map.Entry<AttributeKey<Object>, Object> updateEntry(Map.Entry<String, Object> entry) {
        try {
            return Map.entry((AttributeKey<Object>) ResourceAttributes.class.getField(entry.getKey().toUpperCase().replace('.','_')).get(null), entry.getValue());
        } catch (Throwable e) {
            throw new IllegalArgumentException("No such resource attribute:"+entry.getKey(), e);
        }
    }

    public Attributes getAttributes(String attributeNameRegex) {
        AttributesBuilder builder = Attributes.builder();
        getResource().forEach((key, value)->
        {
            if (key.getKey().matches(attributeNameRegex))
            {
                builder.put(key, value);
            }
        });
        return builder.build();
    }
}
