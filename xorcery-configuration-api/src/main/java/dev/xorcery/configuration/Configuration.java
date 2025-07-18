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
package dev.xorcery.configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import dev.xorcery.builders.With;
import dev.xorcery.json.JsonElement;
import dev.xorcery.json.JsonResolver;
import dev.xorcery.util.Resources;

import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * @author rickardoberg
 * @since 20/04/2022
 */
public record Configuration(ObjectNode json)
        implements JsonElement {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static Configuration empty() {
        return new Configuration(JsonNodeFactory.instance.objectNode());
    }

    public static Supplier<RuntimeException> missing(Object name) {
        return () -> new IllegalArgumentException("Missing configuration setting '" + name + "'");
    }

    public static Builder newConfiguration(){
        return new Builder();
    }

    public record Builder(ObjectNode builder)
            implements With<Builder> {

        public Builder() {
            this(JsonNodeFactory.instance.objectNode());
        }

        void navigateToParentOfPropertyNameThenAdd(JsonNode node, String name, JsonNode value) {
            int i = name.indexOf(".");
            if (i == -1) {
                // name is the last element in path to navigate, navigation complete.
                if (node.path(name).isArray()) {
                    ((ArrayNode) node.get(name)).add(value);
                } else {
                    if (node instanceof ObjectNode object){
                        object.set(name, value);
                    }
                }
                return;
            }
            String childElement = name.substring(0, i);
            String remainingName = name.substring(i + 1);
            JsonNode child = switch (node){
                case ObjectNode object -> object.get(childElement);
                case ArrayNode array -> {
                    for (JsonNode jsonNode : array) {
                        Iterator<JsonNode> valueIterator = jsonNode.values();
                        if (valueIterator.hasNext() && valueIterator.next() instanceof TextNode textNode){
                            if (textNode.asText().equals(childElement) && jsonNode instanceof ObjectNode objectNode)
                                yield objectNode;
                        }
                    }
                    throw new IllegalArgumentException("No object found in array with identifying property valued:"+childElement);
                }
                default -> throw new RuntimeException("Attempted to navigate through json key that already exists, but is not a json-object that support navigation.");
            };
            if (child == null || child.isNull()) {
                // non-existent json-node, need to create child object node
                ObjectNode childObjectNode = switch (node){
                    case ObjectNode object -> object.putObject(childElement);
                    case ArrayNode array -> {
                        ObjectNode arrayChild = JsonNodeFactory.instance.objectNode();
                        array.add(arrayChild);
                        yield arrayChild;
                    }
                    default -> throw new IllegalArgumentException("Unknown node type:"+node);
                };
                navigateToParentOfPropertyNameThenAdd(childObjectNode, remainingName, value);
            } else {
                navigateToParentOfPropertyNameThenAdd(child, remainingName, value);
            }
        }

        public Builder add(String name, JsonNode value) {
            navigateToParentOfPropertyNameThenAdd(builder, name, value);
            return this;
        }

        public Builder add(String name, String value) {
            return add(name, builder.textNode(value));
        }

        public Builder add(String name, long value) {
            return add(name, builder.numberNode(value));
        }

        public Builder add(String name, double value) {
            return add(name, builder.numberNode(value));
        }

        public Builder add(String name, boolean value) {
            return add(name, builder.booleanNode(value));
        }

        public Builder add(String name, Object value) {
            return add(name, mapper.valueToTree(value));
        }

        public Builder remove(String name) {
            builder.remove(name);
            return this;
        }

        public Configuration build() {
            // Resolve any references
            return new Configuration(new JsonResolver().apply(builder, builder));
        }

        @Override
        public String toString() {
            ObjectWriter objectWriter = new YAMLMapper().writer().withDefaultPrettyPrinter();
            try {
                return objectWriter.writeValueAsString(builder);
            } catch (JsonProcessingException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    public Configuration getConfiguration(String name) {
        return getJson(name)
                .filter(ObjectNode.class::isInstance)
                .map(ObjectNode.class::cast).map(Configuration::new)
                .orElseGet(() -> new Configuration(JsonNodeFactory.instance.objectNode()));
    }

    public Stream<Configuration> getConfigurations(String name) {
        return getJson(name)
                .filter(ArrayNode.class::isInstance)
                .map(ArrayNode.class::cast)
                .map(a -> JsonElement.getValuesAs(a, Configuration::new))
                .stream()
                .flatMap(Collection::stream);
    }

    /**
     * Retrieve a configuration value that represents a resource path URL.
     * It will try to resolve the name using ClassLoader.getSystemResource(path), to find the actual
     * path of the resource, given the current classpath and modules.
     *
     * @param name
     * @return
     */
    public Optional<URL> getResourceURL(String name) {
        return getString(name).flatMap(Resources::getResource);
    }

    public Builder asBuilder() {
        return new Builder(json);
    }

    @Override
    public String toString() {
        ObjectWriter objectWriter = new YAMLMapper().writer().withDefaultPrettyPrinter();
        try {
            return objectWriter.writeValueAsString(json);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }
}

