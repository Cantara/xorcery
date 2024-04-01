package com.exoreaction.xorcery.reactivestreams.api;

import com.exoreaction.xorcery.metadata.Metadata;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
class MetadataByteBufferTest {

    @Test
    public void serializeDeserializeJson() throws IOException {
        MetadataByteBuffer outputMetadataByteBuffer = new MetadataByteBuffer(new Metadata.Builder().add("foo", "bar").build(), ByteBuffer.wrap("test".getBytes(StandardCharsets.UTF_8)));

        ObjectMapper objectMapper = new ObjectMapper();
        byte[] output = objectMapper.writeValueAsBytes(outputMetadataByteBuffer);

        MetadataByteBuffer inputMetadataByteBuffer = objectMapper.readValue(output, MetadataByteBuffer.class);

        Assertions.assertEquals(outputMetadataByteBuffer.metadata().json(), inputMetadataByteBuffer.metadata().metadata());
    }
}