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
package com.exoreaction.xorcery.core;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.json.JsonElement;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.logging.log4j.LogManager;
import org.glassfish.hk2.api.PopulatorPostProcessor;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.DescriptorImpl;

import java.util.*;
import java.util.function.Function;

/**
 * This processor checks configuration for the following for each service:
 * servicename.enabled = true/false (whether to load this service)
 * servicename.metadata = object with name/value pairs (additional service metadata, potentially overriding annotations)
 *
 * @param configuration
 */
public record ConfigurationPostPopulatorProcessor(Configuration configuration)
        implements PopulatorPostProcessor {
    @Override
    public DescriptorImpl process(ServiceLocator serviceLocator, DescriptorImpl descriptorImpl) {

        String name = descriptorImpl.getName();
        if (name != null) {
            String enabledFlag = Optional.ofNullable(descriptorImpl.getMetadata().get("enabled"))
                    .flatMap(l -> l.stream().findFirst())
                    .orElseGet(() -> name + ".enabled");

            // Check if we should use defaults
            if (!configuration.has(enabledFlag)) {
                enabledFlag = "defaults.enabled";
            }

            if (configuration.getFalsy(enabledFlag)
                    .orElseGet(() -> false)) {
                configuration.getConfiguration(name).getObjectAs("metadata", object ->
                {
                    Map<String, List<String>> metadatas = new HashMap<>();
                    Function<JsonNode, List<String>> mapper = new Function<>() {
                        @Override
                        public List<String> apply(JsonNode json) {
                            if (json instanceof ArrayNode an) {
                                List<String> values = new ArrayList<>();
                                an.forEach(node -> values.addAll(apply(node)));
                                return values;
                            } else {
                                return List.of(json.asText());
                            }
                        }
                    };
                    JsonElement.toFlatMap(metadatas, "", object, mapper);
                    return metadatas;
                }).ifPresent(descriptorImpl::addMetadata);
                LogManager.getLogger(getClass()).debug("Enabled " + descriptorImpl.getImplementation() + "(" + enabledFlag + "=true)");
                return descriptorImpl;
            } else {
                LogManager.getLogger(getClass()).debug("Disabled " + descriptorImpl.getImplementation() + "(" + enabledFlag + "=false)");
                return null;
            }
        } else {
            return descriptorImpl;
        }
    }
}
