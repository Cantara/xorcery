package com.exoreaction.xorcery.disruptor.reactor;

import com.exoreaction.xorcery.configuration.builder.ConfigurationBuilder;
import com.exoreaction.xorcery.reactivestreams.disruptor.DisruptorConfiguration;
import com.exoreaction.xorcery.reactivestreams.disruptor.SmartBatching;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

class SmartBatchingTest {

    @Test
    public void testSmartBatching() {
        AtomicLong counter = new AtomicLong();
        AtomicLong requests = new AtomicLong();
        AtomicInteger minSize = new AtomicInteger(4096);
        AtomicInteger maxSize = new AtomicInteger();
        DisruptorConfiguration configuration = new DisruptorConfiguration(new ConfigurationBuilder().addYaml("size: 1024").build());
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
                .transformDeferredContextual(new SmartBatching<>(configuration, (list, sink) ->
                {
//                    System.out.println(list.size());
                    minSize.set(Math.min(minSize.get(), list.size()));
                    maxSize.set(Math.max(maxSize.get(), list.size()));
                    try {
                        Thread.sleep(10);
                        sink.next(list);
                    } catch (InterruptedException e) {
                        sink.error(e);
                    }
                }))
//                .doOnRequest(r -> System.out.println("Disruptor request:"+r))
                .doOnNext(value -> {
                })
                .doOnRequest(r -> System.out.println("Subscriber request:" + r))
                .doOnNext(v -> counter.incrementAndGet())
                .blockLast();

        System.out.println("Counter:" + counter.get());
        System.out.println("Min batch size:" + minSize.get());
        System.out.println("Max batch size:" + maxSize.get());
    }
}