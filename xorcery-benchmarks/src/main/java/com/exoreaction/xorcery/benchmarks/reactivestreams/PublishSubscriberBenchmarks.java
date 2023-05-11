package com.exoreaction.xorcery.benchmarks.reactivestreams;

import com.exoreaction.xorcery.configuration.builder.StandardConfigurationBuilder;
import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.configuration.model.InstanceConfiguration;
import com.exoreaction.xorcery.core.Xorcery;
import com.exoreaction.xorcery.metadata.Metadata;
import com.exoreaction.xorcery.service.reactivestreams.api.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.net.URI;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@State(Scope.Benchmark)
@Fork(value = 1, warmups = 1, jvmArgs="-Dcom.sun.management.jmxremote=true" )
@Warmup(iterations = 1)
@Measurement(iterations = 10)
public class PublishSubscriberBenchmarks {

    private final Logger logger = LogManager.getLogger(getClass());

    private static final String config = """
            reactivestreams.enabled: true
            keystores.enabled: true
            jetty.client.enabled: true
            jetty.client.ssl.enabled: true
            jetty.server.enabled: true
            jetty.server.ssl.enabled: true
            jetty.server.ssl.port: 8443
            dns.client.enabled: true
            
            metrics.enabled: true
            metrics.filter:
                - xorcery:*
            metrics.subscriber.authority: localhost:443    
            """;
    private ClientPublisher<WithMetadata<String>> clientPublisher;
    private CompletableFuture<Void> stream;

    public static void main(String[] args) throws Exception {

        new Runner(new OptionsBuilder()
                .include(PublishSubscriberBenchmarks.class.getSimpleName() + ".*")
                .forks(0)
                .jvmArgs("-Dcom.sun.management.jmxremote=true")
                .build()).run();
    }

    private Xorcery xorcery;

    ArrayBlockingQueue<String> queue = new ArrayBlockingQueue<>(10000);
    AtomicInteger writeCount = new AtomicInteger();

    @Setup()
    public void setup() throws Exception {

        Configuration configuration = new Configuration.Builder().with(new StandardConfigurationBuilder().addTestDefaultsWithYaml(config)).build();
        logger.info(StandardConfigurationBuilder.toYaml(configuration));
        xorcery = new Xorcery(configuration);
        ReactiveStreamsServer reactiveStreams = xorcery.getServiceLocator().getService(ReactiveStreamsServer.class);

        // Server subscriber
        ServerSubscriber<WithMetadata<String>> serverSubscriber = new ServerSubscriber<>() {
            @Override
            public void onNext(WithMetadata<String> item) {
                subscription.request(1);
            }
        };

        reactiveStreams.subscriber("serversubscriber", cfg -> serverSubscriber, (Class<? extends Flow.Subscriber<?>>) serverSubscriber.getClass());

        // Client publisher
        clientPublisher = new ClientPublisher<>() {
            @Override
            protected void publish(Flow.Subscriber<? super WithMetadata<String>> subscriber) {
                logger.info("Publish");

                try {
                    while (writeCount.decrementAndGet() > 0) {
                        subscriber.onNext(new WithMetadata<>(new Metadata.Builder()
                                .add("foo", "bar")
                                .build(), queue.take()));
//                        System.out.println("onNext");
                    }
                } catch (InterruptedException e) {
                    // Ignore
                }
                logger.info("onComplete");
                subscriber.onComplete();
            }
        };

        ReactiveStreamsClient reactiveStreamsClient = xorcery.getServiceLocator().getService(ReactiveStreamsClient.class);
        InstanceConfiguration standardConfiguration = new InstanceConfiguration(xorcery.getServiceLocator().getService(Configuration.class).getConfiguration("instance"));
        URI serverUri = standardConfiguration.getURI();
        stream = reactiveStreamsClient.publish(serverUri.getAuthority(), "serversubscriber",
                Configuration::empty, clientPublisher, (Class<? extends Flow.Publisher<?>>) clientPublisher.getClass(), ClientConfiguration.defaults());

        logger.info("Setup done");
    }

    @TearDown
    public void teardown() throws Exception {
        clientPublisher.getDone().get();
        stream.complete(null);
        xorcery.close();
        logger.info("Teardown done");
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void writes() throws ExecutionException, InterruptedException, TimeoutException {
        queue.put(System.currentTimeMillis() + "");
        writeCount.incrementAndGet();
    }

    public static abstract class ServerSubscriber<T>
            implements Flow.Subscriber<T> {

        protected Flow.Subscription subscription;

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            subscription.request(8192);
        }

        @Override
        public void onError(Throwable throwable) {

        }

        @Override
        public void onComplete() {

        }
    }

    public static abstract class ClientPublisher<T>
            implements Flow.Publisher<T> {

        private final CompletableFuture<Void> done = new CompletableFuture<>();

        public CompletableFuture<Void> getDone() {
            return done;
        }

        @Override
        public void subscribe(Flow.Subscriber<? super T> subscriber) {
            Semaphore semaphore = new Semaphore(0);
            AtomicLong initialRequest = new AtomicLong(-1);

            subscriber.onSubscribe(new Flow.Subscription() {
                @Override
                public void request(long n) {
                    semaphore.release((int) n);
                    if (initialRequest.get() == -1) {
                        initialRequest.set(n);
                    }
                }

                @Override
                public void cancel() {

                }
            });

            CompletableFuture.runAsync(() ->
            {
                publish(subscriber);
            }).whenComplete((r, t) -> done.complete(null));
        }

        protected abstract void publish(Flow.Subscriber<? super T> subscriber);
    }

}
