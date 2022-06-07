package com.exoreaction.reactiveservices.disruptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;

class MetadataTest {

    @Test
    public void testSerializeDeserialize() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        Metadata metadata = new Metadata.Builder()
                .add("foo", "bar")
                .build();

        String value = objectMapper.writeValueAsString(metadata);

        System.out.println(value);

        Metadata metadata2 = objectMapper.readValue(new StringReader(value), Metadata.class);

        System.out.println(metadata2);
    }

}