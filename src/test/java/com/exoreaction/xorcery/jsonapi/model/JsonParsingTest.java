package com.exoreaction.xorcery.jsonapi.model;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.util.ByteBufferBackedInputStream;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.jetty.io.ArrayByteBufferPool;
import org.eclipse.jetty.io.ByteBufferOutputStream2;
import org.eclipse.jetty.io.ByteBufferPool;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class JsonParsingTest {

    private static final String metadataJson = """
    {"some":"key"}
    """;

    @Test
    public void objectMapperTest() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        JsonNode node = objectMapper.reader().readTree(getClass().getResourceAsStream("/grouptemplates.json"));

        objectMapper.writer().writeValues(System.out).write(node);
    }

    @Test
    public void jsr353Test() throws IOException {
        JsonObject jsonObject = Json.createReader(getClass().getResourceAsStream("/grouptemplates.json")).readObject();
        Json.createWriter(System.out).write(jsonObject);
    }

    @Test
    public void serialSerializationObjectMapperTest() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
        ByteBufferPool pool = new ArrayByteBufferPool();

        ByteBufferOutputStream2 bout = new ByteBufferOutputStream2(pool, false);

        JsonNode metadata = objectMapper.reader().readTree(metadataJson);
        byte[] value = getClass().getResourceAsStream("/grouptemplates.json").readAllBytes();

        ObjectWriter writer = objectMapper.writer();
        writer.writeValue(bout, metadata);
        bout.write(value);
//        writer.writeValue(bout, value);


        ByteBuffer byteBuffer =bout.takeByteBuffer();

        System.out.println("BOUT:"+new String(byteBuffer.array()));

        InputStream bin = new ByteBufferBackedInputStream(byteBuffer);

        JsonFactory jf = new JsonFactory(objectMapper);
        JsonParser jp = jf.createParser(bin);
        JsonToken metadataToken = jp.nextToken();
        JsonNode metadataIn = jp.readValueAsTree();
        long location = jp.getCurrentLocation().getByteOffset();

        byteBuffer.position((int)location);
        ByteBufferBackedInputStream inputStream = new ByteBufferBackedInputStream(byteBuffer);
        byte[] dataIn = inputStream.readAllBytes();

        System.out.println("1:"+metadataIn);
        System.out.println("2:"+new String(dataIn));
    }
}
