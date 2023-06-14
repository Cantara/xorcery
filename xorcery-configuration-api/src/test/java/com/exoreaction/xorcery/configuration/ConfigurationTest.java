package com.exoreaction.xorcery.configuration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class ConfigurationTest {

    @Test
    void addArrayElements() {
        Configuration.Builder builder = new Configuration.Builder();
        builder.add("root.fruits", JsonNodeFactory.instance.arrayNode());
        builder.add("root.fruits", "apple");
        builder.add("root.fruits", "orange");
        Configuration config = builder.build();
        List<String> list = config.getListAs("root.fruits", JsonNode::textValue).orElseThrow();
        Assertions.assertEquals(List.of("apple", "orange"), list);
    }
}