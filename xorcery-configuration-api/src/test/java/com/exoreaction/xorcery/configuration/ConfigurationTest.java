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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
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
}