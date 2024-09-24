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

        String result = objectMapper.writeValueAsString( merged );

        ObjectNode expectedResult = (ObjectNode)objectMapper.readTree(getClass().getResource("jsonmergertestresult.yaml"));

        System.out.println(result);
        assertThat(result, equalTo(objectMapper.writeValueAsString( expectedResult)));
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

    @Test
    public void testMergeIntoEmptyArray() throws Throwable {
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

        ObjectNode test1 = (ObjectNode)objectMapper.readTree("""
                jersey:
                    server:
                        register: []
                """);
        ObjectNode test2 = (ObjectNode)objectMapper.readTree("""
                jersey:
                    server:
                        register:
                        - bar
                """);
        ObjectNode merged = new JsonMerger().apply(test1, test2);

        String result = objectMapper.writeValueAsString(merged);

        String expectedResult = """
                ---
                jersey:
                  server:
                    register:
                    - "bar"
                """;

        assertThat(result, equalTo(expectedResult));
//        System.out.println(result);
    }

    @Test
    public void testMergeStringIntoArray() throws Throwable {
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

        ObjectNode test1 = (ObjectNode)objectMapper.readTree("""
                jersey:
                    server:
                        register:  
                        - foo
                        - bar
                """);
        ObjectNode test2 = (ObjectNode)objectMapper.readTree("""
                jersey:
                    server:
                        register: bar
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

    @Test
    public void testMergeArrayIntoString() throws Throwable {
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

        ObjectNode test1 = (ObjectNode)objectMapper.readTree("""
                jersey:
                    server:
                        register: bar  
                """);
        ObjectNode test2 = (ObjectNode)objectMapper.readTree("""
                jersey:
                    server:
                        register: 
                        - foo
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

    @Test
    public void testMergeReferenceIntoArray() throws Throwable {
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

        ObjectNode test1 = (ObjectNode)objectMapper.readTree("""
                jersey:
                    server:
                        register:
                        - bar  
                """);
        ObjectNode test2 = (ObjectNode)objectMapper.readTree("""
                jersey:
                    server:
                        register: "{{ foo }}"
                """);
        ObjectNode merged = new JsonMerger().apply(test1, test2);

        String result = objectMapper.writeValueAsString(merged);

        String expectedResult = """
                ---
                jersey:
                  server:
                    register: "{{ foo }}"
                """;

        assertThat(result, equalTo(expectedResult));
        System.out.println(result);
    }


}