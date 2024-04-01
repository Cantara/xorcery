package com.exoreaction.xorcery.reactivestreams.api.server;

import org.reactivestreams.Publisher;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.util.function.Function;

public interface WebSocketStreamsServer {
    <PUBLISH> Disposable publisher(
            String path,
            Class<? super PUBLISH> publishType,
            Publisher<PUBLISH> publisher)
            throws IllegalArgumentException;

    <SUBSCRIBE> Disposable subscriber(
            String path,
            Class<? super SUBSCRIBE> subscribeType,
            Function<Flux<SUBSCRIBE>, Publisher<SUBSCRIBE>> subscriberTransform)
            throws IllegalArgumentException;

    <SUBSCRIBE, RESULT> Disposable subscriberWithResult(
            String path,
            Class<? super SUBSCRIBE> subscribeType,
            Class<? super RESULT> resultType,
            Function<Flux<SUBSCRIBE>, Publisher<RESULT>> subscriberWithResultTransform)
            throws IllegalArgumentException;
}
