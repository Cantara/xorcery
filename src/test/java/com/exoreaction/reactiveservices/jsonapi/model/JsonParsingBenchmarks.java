package com.exoreaction.reactiveservices.jsonapi.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@State(Scope.Benchmark)
@Fork(value = 1, warmups = 1)
@Warmup(iterations = 1)
@Measurement(iterations = 1)
public class JsonParsingBenchmarks {

    private JsonNode jsonNode;
    private JsonObject jsonObject;
    private ObjectMapper objectMapper;
    private ByteArrayOutputStream baos;

    public static void main(String[] args) throws Exception {

        new Runner(new OptionsBuilder()
                .include(JsonParsingBenchmarks.class.getSimpleName()+".*Writes")
                .forks(1)
                .build()).run();
    }

    private ObjectReader objectReader;

    @Setup()
    public void setup() throws Exception {
        objectMapper = new ObjectMapper();
        objectReader = objectMapper.reader();
        jsonNode = objectReader.readTree(getClass().getResourceAsStream("/grouptemplates.json"));

        jsonObject = Json.createReader(getClass().getResourceAsStream("/grouptemplates.json")).readObject();

        baos = new ByteArrayOutputStream();

        System.out.println("Setup done");
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void objectMapperReads() throws IOException {
        JsonNode node = objectReader.readTree(getClass().getResourceAsStream("/grouptemplates.json"));
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void json353Reads() throws ExecutionException, InterruptedException, TimeoutException {
        JsonObject jsonObject = Json.createReader(getClass().getResourceAsStream("/grouptemplates.json")).readObject();
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void objectMapperWrites() throws IOException {
        objectMapper.writer().writeValues(baos).write(jsonNode);
        baos.reset();
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void json353Writes() throws ExecutionException, InterruptedException, TimeoutException {
        Json.createWriter(baos).write(jsonObject);
        baos.reset();
    }

}
