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
package com.exoreaction.xorcery.configuration.providers;

import com.exoreaction.xorcery.configuration.spi.ConfigurationProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import java.io.UncheckedIOException;
import java.util.Optional;

public class EnvironmentVariablesConfigurationProvider
        implements ConfigurationProvider {

    private final YAMLMapper yamlMapper;

    public EnvironmentVariablesConfigurationProvider() {
        yamlMapper = new YAMLMapper();
    }

    @Override
    public String getNamespace() {
        return "ENV";
    }

    @Override
    public JsonNode getJson(String name) {
        return Optional.ofNullable(System.getenv(name))
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
        return "Environment variables";
    }
}
