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
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import dev.xorcery.configuration.spi.ConfigurationProvider;
import dev.xorcery.util.Resources;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

public class ResourceConfigurationProvider
        implements ConfigurationProvider {

    private final ObjectMapper jsonMapper = new JsonMapper().findAndRegisterModules();
    private final ObjectMapper yamlMapper = new YAMLMapper().findAndRegisterModules();

    @Override
    public String getNamespace() {
        return "RESOURCE";
    }

    @Override
    public JsonNode getJson(String resourceName) {
        return switch (resourceName)
        {
            case "yaml"->new ResourceLoader(new YamlResourceParser(),"");
            case "json"->new ResourceLoader(new JsonResourceParser(),"");
            case "string"->new ResourceLoader(new StringResourceParser(),"");
            default -> new ResourceLoader(new StringResourceParser(), "");
        };
    }

    private static class ResourceLoader
        extends ObjectNode
    {
        private final Function<String, JsonNode> resourceParser;
        private final String prefix;

        public ResourceLoader(Function<String, JsonNode> resourceParser, String prefix) {
            super(JsonNodeFactory.instance);
            this.resourceParser = resourceParser;
            this.prefix = prefix;
        }

        @Override
        public JsonNode get(String propertyName) {
            return Resources.getResource(prefix+"."+propertyName).map(url ->
            {
                try (InputStream inputStream = url.openStream()) {
                    String resource = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                    return resourceParser.apply(resource);
                } catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                }
            }).orElseGet(()->
            {
                return new ResourceLoader(resourceParser, prefix.isEmpty() ? propertyName : prefix+"."+propertyName);
            });
        }
    }

    private class YamlResourceParser
        implements Function<String, JsonNode>
    {
        @Override
        public JsonNode apply(String resource) {
            try {
                return yamlMapper.readTree(resource);
            } catch (JsonProcessingException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    private class JsonResourceParser
        implements Function<String, JsonNode>
    {
        @Override
        public JsonNode apply(String resource) {
            try {
                return jsonMapper.readTree(resource);
            } catch (JsonProcessingException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    private static class StringResourceParser
        implements Function<String, JsonNode>
    {
        @Override
        public JsonNode apply(String string) {
            return JsonNodeFactory.instance.textNode(string);
        }
    }

    @Override
    public String toString() {
        return "Resource configuration";
    }
}
