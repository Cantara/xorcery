package com.exoreaction.xorcery.core;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.json.model.JsonElement;
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

        if (((Object) descriptorImpl.getName()) instanceof String name) {
            if (configuration.getBoolean(name + ".enabled")
                    .orElseGet(() -> configuration.getBoolean("defaults.enabled").orElse(false))) {
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
                return descriptorImpl;
            } else {
                LogManager.getLogger(getClass()).debug("Disabled "+name);
                return null;
            }
        } else {
            return descriptorImpl;
        }
    }
}
