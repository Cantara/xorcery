package com.exoreaction.reactiveservices.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Test;

import java.io.IOException;

class JsonMergerTest {

    @Test
    public void testMerge() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

        ObjectNode test1 = (ObjectNode)objectMapper.readTree(getClass().getResource("jsonmergertest1.yaml"));
        ObjectNode test2 = (ObjectNode)objectMapper.readTree(getClass().getResource("jsonmergertest2.yaml"));
        ObjectNode merged = new JsonMerger().apply(test1, test2);

        objectMapper.writeValue(System.out, merged);

    }
}