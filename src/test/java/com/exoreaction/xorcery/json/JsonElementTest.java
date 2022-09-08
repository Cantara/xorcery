package com.exoreaction.xorcery.json;

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
    }
}