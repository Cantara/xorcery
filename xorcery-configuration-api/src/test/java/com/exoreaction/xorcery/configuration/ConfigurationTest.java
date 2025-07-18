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
package com.exoreaction.xorcery.configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import dev.xorcery.configuration.Configuration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class ConfigurationTest {

    @Test
    void addArrayElements() {
        Configuration.Builder builder = Configuration.newConfiguration();
        builder.add("root.fruits", JsonNodeFactory.instance.arrayNode());
        builder.add("root.fruits", "apple");
        builder.add("root.fruits", "orange");
        Configuration config = builder.build();
        List<String> list = config.getListAs("root.fruits", JsonNode::textValue).orElseThrow();
        Assertions.assertEquals(List.of("apple", "orange"), list);
    }

    @Test
    void overrideNamedArrayElements() throws JsonProcessingException {
        JsonNode jsonNode = new YAMLMapper().readTree("""
                clients:
                - name: client1
                  foo: 3
                - name: client2
                  foo: 4
                - name: client3
                  foo: 5
                """);
        Configuration.Builder builder = new Configuration.Builder((ObjectNode) jsonNode);

        builder.add("clients.client2.foo", JsonNodeFactory.instance.numberNode(7));

        Configuration config = builder.build();
        String toString = new JsonMapper().writeValueAsString(config.json());
        String expected = "{\"clients\":[{\"name\":\"client1\",\"foo\":3},{\"name\":\"client2\",\"foo\":7},{\"name\":\"client3\",\"foo\":5}]}";
        Assertions.assertEquals(expected, toString);
    }
}