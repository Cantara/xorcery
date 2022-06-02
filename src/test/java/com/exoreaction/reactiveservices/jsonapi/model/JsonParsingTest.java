package com.exoreaction.reactiveservices.jsonapi.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class JsonParsingTest {

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
}
