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
package dev.xorcery.reactivestreams.extras.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import dev.xorcery.reactivestreams.extras.publishers.ResourcePublisherContext;
import dev.xorcery.reactivestreams.extras.publishers.YamlPublisher;
import dev.xorcery.reactivestreams.extras.subscribers.ResourceSubscriberContext;
import dev.xorcery.reactivestreams.extras.subscribers.SaveToFile;
import dev.xorcery.util.Resources;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SynchronousSink;
import reactor.util.context.Context;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

public class CopyFileTest {

    ObjectMapper mapper = new YAMLMapper().findAndRegisterModules();

    @Test
    void copyFile(@TempDir File tempDirectory) {

        YamlPublisher<Map<String, Object>> filePublisher = new YamlPublisher<>(Map.class);

        File outFile = new File(tempDirectory, "test.yaml");
//        File outFile = new File("test.yaml").getAbsoluteFile();
        List<Map<String, Object>> result = Flux
                .from(filePublisher)
                .transformDeferredContextual(new SaveToFile<>(this::mapToByteBuffer))
                .contextWrite(Context.of(
                        ResourcePublisherContext.resourceUrl, Resources.getResource("testevents.yaml").orElseThrow(),
                        ResourceSubscriberContext.file, outFile
                ))
                .toStream()
                .toList();

        Assertions.assertEquals(outFile.length(), 204);
    }

    private <T> void mapToByteBuffer(T item, SynchronousSink<ByteBuffer> sink) {
        try {
            sink.next(ByteBuffer.wrap(mapper.writeValueAsBytes(item)));
        } catch (JsonProcessingException e) {
            sink.error(e);
        }
    }
}
