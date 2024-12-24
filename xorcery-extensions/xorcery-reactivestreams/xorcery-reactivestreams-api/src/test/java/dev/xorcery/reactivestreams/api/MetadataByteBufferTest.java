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
package dev.xorcery.reactivestreams.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.xorcery.metadata.Metadata;
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