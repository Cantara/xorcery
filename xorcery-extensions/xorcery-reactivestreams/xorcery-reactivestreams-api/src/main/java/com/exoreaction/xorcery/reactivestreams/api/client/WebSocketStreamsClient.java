package com.exoreaction.xorcery.reactivestreams.api.client;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.util.context.Context;
import reactor.util.context.ContextView;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Use these methods to create Flux instance for the various operations.
 * The methods without a specific server URI to connect to must be supplied via {@link reactor.util.context.ContextView}
 * under the key {@link WebSocketStreamContext#serverUri} as a {@link URI} or {@link String}.
 */
public interface WebSocketStreamsClient {

    <PUBLISH> Function<Flux<PUBLISH>, Publisher<PUBLISH>> publish(
            WebSocketClientOptions options,
            Class<? super PUBLISH> publishType,
            String... publishContentTypes
    );

    <PUBLISH, RESULT> Function<? super Flux<PUBLISH>, Publisher<RESULT>> publishWithResult(
            WebSocketClientOptions options,
            Class<? super PUBLISH> publishType,
            Class<? super RESULT> resultType,
            Collection<String> messageContentTypes,
            Collection<String> resultContentTypes
    );

    default <PUBLISH, RESULT> Function<? super Flux<PUBLISH>, Publisher<RESULT>> publishWithResult(
            WebSocketClientOptions options,
            Class<? super PUBLISH> publishType,
            Class<? super RESULT> resultType,
            String messageContentType,
            String resultContentType
    )
    {
        return publishWithResult(options, publishType, resultType, List.of(messageContentType), List.of(resultContentType));
    }

    default <PUBLISH, RESULT> Function<? super Flux<PUBLISH>, Publisher<RESULT>> publishWithResult(
            WebSocketClientOptions options,
            Class<? super PUBLISH> publishType,
            Class<? super RESULT> resultType
    ){
        return publishWithResult(options, publishType, resultType, Collections.emptyList(), Collections.emptyList());
    }

    <SUBSCRIBE> Flux<SUBSCRIBE> subscribe(
            WebSocketClientOptions options,
            Class<? super SUBSCRIBE> subscribeType,
            String... subscribeContentTypes
    );
}
