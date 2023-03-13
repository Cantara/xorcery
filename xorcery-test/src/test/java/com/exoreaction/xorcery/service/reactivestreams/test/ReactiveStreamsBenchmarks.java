package com.exoreaction.xorcery.service.reactivestreams.test;

import com.exoreaction.xorcery.configuration.builder.StandardConfigurationBuilder;
import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.configuration.model.StandardConfiguration;
import com.exoreaction.xorcery.core.Xorcery;
import com.exoreaction.xorcery.metadata.Metadata;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreamsClient;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreamsServer;
import com.exoreaction.xorcery.service.reactivestreams.api.WithMetadata;
import com.exoreaction.xorcery.service.reactivestreams.api.WithResult;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.net.URI;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@State(Scope.Benchmark)
@Fork(value = 1, warmups = 1)
@Warmup(iterations = 1)
@Measurement(iterations = 10)
public class ReactiveStreamsBenchmarks {

    private static final String config = """
            reactivestreams.enabled: true
            keystores.enabled: true
            dns.client.discovery.enabled: false
            jetty.client.ssl.enabled: true
            """;
    private ClientPublisher<WithResult<WithMetadata<String>, Integer>> clientPublisher;

    public static void main(String[] args) throws Exception {

        new Runner(new OptionsBuilder()
//                .include(ReactiveStreamsBenchmarks.class.getSimpleName() + ".writes")
                .forks(1)
                .build()).run();
    }

    private Xorcery xorcery;

    ArrayBlockingQueue<String> queue = new ArrayBlockingQueue<>(512);
    AtomicInteger total = new AtomicInteger();

    @Setup()
    public void setup() throws Exception {

        Configuration configuration = new Configuration.Builder().with(new StandardConfigurationBuilder().addTestDefaultsWithYaml(config)).build();
        xorcery = new Xorcery(configuration);
        ReactiveStreamsServer reactiveStreams = xorcery.getServiceLocator().getService(ReactiveStreamsServer.class);

        // Server subscriber
        ServerSubscriber<WithResult<WithMetadata<String>, Integer>> serverSubscriber = new ServerSubscriber<>() {
            @Override
            public void onNext(WithResult<WithMetadata<String>, Integer> item) {
                item.result().complete(item.event().event().length());
                subscription.request(1);
            }
        };

        reactiveStreams.subscriber("serversubscriber", cfg -> serverSubscriber, (Class<? extends Flow.Subscriber<?>>) serverSubscriber.getClass());

        // Client publisher
        clientPublisher = new ClientPublisher<>() {
            @Override
            protected void publish(Flow.Subscriber<? super WithResult<WithMetadata<String>, Integer>> subscriber) {

                try {
                    for (int i = 0; i < 100; i++) {
                        CompletableFuture<Integer> future = new CompletableFuture<>();
                        future.whenComplete((r, t) ->
                        {
                            total.addAndGet(r);
                        });
                        subscriber.onNext(new WithResult<>(new WithMetadata<>(new Metadata.Builder()
                                .add("foo", "bar")
                                .build(), queue.take()), future));
                    }
                } catch (InterruptedException e) {
                    // Ignore
                }
                subscriber.onComplete();
            }
        };

        ReactiveStreamsClient reactiveStreamsClient = xorcery.getServiceLocator().getService(ReactiveStreamsClient.class);
        StandardConfiguration standardConfiguration = () -> xorcery.getServiceLocator().getService(Configuration.class);
        URI serverUri = standardConfiguration.getServerUri();
        reactiveStreamsClient.publish(serverUri.getAuthority(), "serversubscriber",
                Configuration::empty, clientPublisher, (Class<? extends Flow.Publisher<?>>) clientPublisher.getClass(), Configuration.empty());

        System.out.println("Setup done");
    }

    @TearDown
    public void teardown() throws Exception {
        clientPublisher.getDone().get();

        xorcery.close();
        System.out.println("Teardown done");
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    public void writes() throws ExecutionException, InterruptedException, TimeoutException {
        queue.add(System.currentTimeMillis() + "");
    }

    public static abstract class ServerSubscriber<T>
            implements Flow.Subscriber<T> {

        protected Flow.Subscription subscription;

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            subscription.request(1);
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
