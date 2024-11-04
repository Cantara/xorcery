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
package dev.xorcery.configuration.providers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import dev.xorcery.configuration.spi.ConfigurationProvider;

import java.io.UncheckedIOException;
import java.util.Optional;

public class SystemPropertiesConfigurationProvider
        implements ConfigurationProvider {

    private final ObjectMapper yamlMapper;

    public SystemPropertiesConfigurationProvider() {
        yamlMapper = new YAMLMapper().findAndRegisterModules();
    }

    @Override
    public String getNamespace() {
        return "SYSTEM";
    }

    @Override
    public JsonNode getJson(String name) {
        return Optional.ofNullable(System.getProperty(name.replace('_', '.')))
                .map(value ->
                {
                    try {
                        return yamlMapper.readTree(value);
                    } catch (JsonProcessingException e) {
                        throw new UncheckedIOException(e);
                    }
                })
                .orElseGet(MissingNode::getInstance);
    }

    @Override
    public String toString() {
        return "System properties";
    }
}
