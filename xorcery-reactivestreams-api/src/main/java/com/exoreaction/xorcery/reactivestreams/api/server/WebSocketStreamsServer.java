package com.exoreaction.xorcery.reactivestreams.api.server;

import org.reactivestreams.Publisher;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

public interface WebSocketStreamsServer {
    <PUBLISH> Disposable publisher(
            String path,
            String contentType,
            Class<PUBLISH> publishType,
            Publisher<PUBLISH> publisher)
            throws IllegalArgumentException;

    <SUBSCRIBE> Disposable subscriber(
            String path,
            String contentType,
            Class<SUBSCRIBE> subscribeType,
            Function<Flux<SUBSCRIBE>, Publisher<SUBSCRIBE>> appendSubscriber)
            throws IllegalArgumentException;

    <SUBSCRIBE, RESULT> Disposable subscriberWithResult(
            String path,
            String contentType,
            Class<SUBSCRIBE> subscribeType,
            Class<RESULT> resultType,
            Function<Flux<SUBSCRIBE>, Publisher<RESULT>> subscribeAndReturnResult)
            throws IllegalArgumentException;
}
