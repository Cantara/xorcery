package com.exoreaction.xorcery.disruptor.reactor;

import com.exoreaction.xorcery.configuration.builder.ConfigurationBuilder;
import com.exoreaction.xorcery.reactivestreams.disruptor.DisruptorConfiguration;
import com.exoreaction.xorcery.reactivestreams.disruptor.SmartBatchingTransformer;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;
class SmartBatchingTransformerTest {

    @Test
    public void testSmartBatching()
    {
                AtomicLong counter = new AtomicLong();
                AtomicLong requests = new AtomicLong();
                AtomicInteger minSize = new AtomicInteger(4096);
                AtomicInteger maxSize = new AtomicInteger();
                Flux.fromStream(IntStream.range(0, 10000).boxed())
                        .doOnRequest(r -> System.out.println("Source requests:" + requests.addAndGet(r)))
                        .doOnNext(v -> requests.decrementAndGet())
                        .doOnNext(v ->
                        {
                            int mod = 1000;
                            if (v % mod == 0) {
                                System.out.println("Switch " + v);
                            }

                    if ((v / mod) % 2 == 1) {
                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                        })
                        .publishOn(Schedulers.boundedElastic(), 4096)
                        .transformDeferred(new SmartBatchingTransformer<>(new DisruptorConfiguration(new ConfigurationBuilder().addYaml("size: 1024").build())))
//                .doOnRequest(r -> System.out.println("Disruptor request:"+r))
                        .<List<Integer>>handle((batch, sink) ->
                        {

/*
                    try {
                        Thread.sleep(3);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
*/
/*
                    if ((counter.get() / 1000) % 2 == 1) {
                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
*/
                            sink.next(batch);
                        })
                        .doOnNext(value -> {
                            minSize.set(Math.min(minSize.get(), value.size()));
                            maxSize.set(Math.max(maxSize.get(), value.size()));
                        })
                        .flatMapIterable(v->v, 4096)
                        .doOnRequest(r -> System.out.println("Subscriber request:" + r))
                        .doOnNext(v -> counter.incrementAndGet())
                        .subscribeOn(Schedulers.parallel(), true)
                        .blockLast();

                System.out.println("Counter:" + counter.get());
                System.out.println("Min batch size:" + minSize.get());
                System.out.println("Max batch size:" + maxSize.get());
            }
}