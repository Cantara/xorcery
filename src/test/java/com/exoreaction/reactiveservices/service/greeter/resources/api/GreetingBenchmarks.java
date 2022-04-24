package com.exoreaction.reactiveservices.service.greeter.resources.api;

import com.exoreaction.reactiveservices.server.Server;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.Fields;
import org.openjdk.jmh.Main;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@State(Scope.Benchmark)
@Fork(value = 1, warmups = 1)
@Warmup(iterations = 1)
@Measurement(iterations = 10)
public class GreetingBenchmarks {
    public static void main(String[] args) throws Exception {

        new Runner(new OptionsBuilder()
                .include(GreetingBenchmarks.class.getSimpleName()+".writes")
                .forks(1)
                .build()).run();
    }

    private Server server;
    private HttpClient httpClient = new HttpClient();

    @Setup()
    public void setup() throws Exception {
        server = new Server();
        httpClient.start();
        System.out.println("Setup done");
    }

    @TearDown
    public void teardown() throws Exception {
        httpClient.stop();
        server.close();
        System.out.println("Teardown done");
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void reads() throws ExecutionException, InterruptedException, TimeoutException {
        httpClient.GET("http://localhost:8080/api/greeter").getContentAsString();
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void writes() throws ExecutionException, InterruptedException, TimeoutException {
        httpClient.FORM("http://localhost:8080/api/greeter", new Fields() {{
            put("greeting", "HelloWorld!");
        }}).getContentAsString();
    }
}
