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
package com.exoreaction.xorcery.json.test;

import com.exoreaction.xorcery.json.model.JsonElement;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.Map;

class JsonElementTest {

    @Test
    void toMap() throws JsonProcessingException {
        String json = """
                {
                    "foo":"bar",
                    "first":{
                        "second":"2",
                        "third":{
                        "key":"value"
                        }
                    }
                }
                """;

        ObjectNode jsonNode = (ObjectNode)new ObjectMapper().readTree(json);

        Map<String, Object> result = JsonElement.toMap(jsonNode, JsonNode::textValue);

        System.out.println(result);

        // TODO make value of "first" in map be another map, and not null
    }
}