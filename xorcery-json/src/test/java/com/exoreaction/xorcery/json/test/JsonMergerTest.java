package com.exoreaction.xorcery.json.test;

import com.exoreaction.xorcery.json.JsonMerger;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

class JsonMergerTest {

    @Test
    public void testMerge() throws Throwable {
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

        ObjectNode test1 = (ObjectNode)objectMapper.readTree(getClass().getResource("jsonmergertest1.yaml"));
        ObjectNode test2 = (ObjectNode)objectMapper.readTree(getClass().getResource("jsonmergertest2.yaml"));
        ObjectNode merged = new JsonMerger().apply(test1, test2);

        String result = merged.toPrettyString();

        String expectedResult = new String(getClass().getResourceAsStream("jsonmergertestresult.yaml").readAllBytes());

        System.out.println(result);
        assertThat(result, equalTo(expectedResult));
    }

    @Test
    public void testMergeDotNameArrays() throws Throwable {
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

        ObjectNode test1 = (ObjectNode)objectMapper.readTree("""
                jersey:
                    server:
                        register:
                            - foo
                """);
        ObjectNode test2 = (ObjectNode)objectMapper.readTree("""
                jersey.server.register:
                    - bar
                """);
        ObjectNode merged = new JsonMerger().apply(test1, test2);

        String result = objectMapper.writeValueAsString(merged);

        String expectedResult = """
                ---
                jersey:
                  server:
                    register:
                    - "foo"
                    - "bar"
                """;

        assertThat(result, equalTo(expectedResult));
//        System.out.println(result);
    }

}