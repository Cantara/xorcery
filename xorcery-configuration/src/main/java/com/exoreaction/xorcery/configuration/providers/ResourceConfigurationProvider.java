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
import com.exoreaction.xorcery.util.Resources;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import java.io.IOException;
import java.io.UncheckedIOException;

public class ResourceConfigurationProvider
        implements ConfigurationProvider {

    private final JsonMapper jsonMapper = new JsonMapper();
    private final YAMLMapper yamlMapper = new YAMLMapper();

    @Override
    public String getNamespace() {
        return "RESOURCE";
    }

    @Override
    public JsonNode getJson(String resourceName) {
        return new ObjectNode(JsonNodeFactory.instance)
        {
            @Override
            public JsonNode get(String resourceExtension) {
                return Resources.getResource(resourceName+"."+resourceExtension).map(url ->
                {
                    try {
                        if (resourceExtension.equals("yaml") || resourceExtension.equals("yml"))
                        {
                            return yamlMapper.readTree(url);
                        } else if (resourceExtension.equals("json"))
                        {
                            return jsonMapper.readTree(url);
                        } else
                        {
                            throw new IllegalArgumentException(String.format("Resource '%s.%s' is not a JSON or YAML file", resourceName, resourceExtension));
                        }
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }).orElseThrow(()->new IllegalArgumentException(String.format("No resource named '%s.%s' found", resourceName, resourceExtension)));
            }
        };
    }
    @Override
    public String toString() {
        return "Resource configuration";
    }
}
