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
        String json = "{\n" +
                      "    \"foo\":\"bar\",\n" +
                      "    \"first\":{\n" +
                      "        \"second\":\"2\",\n" +
                      "        \"third\":{\n" +
                      "        \"key\":\"value\"\n" +
                      "        }\n" +
                      "    }\n" +
                      "}\n";

        ObjectNode jsonNode = (ObjectNode)new ObjectMapper().readTree(json);

        Map<String, Object> result = JsonElement.toMap(jsonNode, JsonNode::textValue);

        System.out.println(result);

        // TODO make value of "first" in map be another map, and not null
    }
}