package com.exoreaction.reactivestreams.server.extra.yaml.test;

import com.exoreaction.xorcery.reactivestreams.extras.publishers.ResourcePublisherContext;
import com.exoreaction.xorcery.reactivestreams.extras.publishers.YamlPublisher;
import com.exoreaction.xorcery.reactivestreams.extras.subscribers.ResourceSubscriberContext;
import com.exoreaction.xorcery.reactivestreams.extras.subscribers.SaveToFile;
import com.exoreaction.xorcery.util.Resources;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
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

    YAMLMapper mapper = new YAMLMapper();

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
