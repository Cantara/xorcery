package dev.xorcery.reactivestreams.api.client;

import org.reactivestreams.Publisher;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.util.context.ContextView;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * Use these methods to create Flux instance for the various operations.
 * The methods without a specific server URI to connect to must be supplied via {@link reactor.util.context.ContextView}
 * under the key {@link ClientWebSocketStreamContext#serverUri} as a {@link URI} or {@link String}.
 */
public interface ClientWebSocketStreams {

    <PUBLISH> Function<Flux<PUBLISH>, Publisher<PUBLISH>> publish(
            ClientWebSocketOptions options,
            Class<? super PUBLISH> publishType,
            String... publishContentTypes
    );

    <PUBLISH, RESULT> Function<? super Flux<PUBLISH>, Publisher<RESULT>> publishWithResult(
            ClientWebSocketOptions options,
            Class<? super PUBLISH> publishType,
            Class<? super RESULT> resultType,
            Collection<String> messageContentTypes,
            Collection<String> resultContentTypes
    );

    default <PUBLISH, RESULT> Function<? super Flux<PUBLISH>, Publisher<RESULT>> publishWithResult(
            ClientWebSocketOptions options,
            Class<? super PUBLISH> publishType,
            Class<? super RESULT> resultType,
            String messageContentType,
            String resultContentType
    )
    {
        return publishWithResult(options, publishType, resultType, List.of(messageContentType), List.of(resultContentType));
    }

    default <PUBLISH, RESULT> Function<? super Flux<PUBLISH>, Publisher<RESULT>> publishWithResult(
            ClientWebSocketOptions options,
            Class<? super PUBLISH> publishType,
            Class<? super RESULT> resultType
    ){
        return publishWithResult(options, publishType, resultType, Collections.emptyList(), Collections.emptyList());
    }

    <SUBSCRIBE> Flux<SUBSCRIBE> subscribe(
            ClientWebSocketOptions options,
            Class<? super SUBSCRIBE> subscribeType,
            String... subscribeContentTypes
    );

    <SUBSCRIBE, RESULT> Disposable subscribeWithResult(
            ClientWebSocketOptions options,
            Class<? super SUBSCRIBE> subscribeType,
            Class<? super RESULT> resultType,
            Collection<String> subscribeContentTypes,
            Collection<String> resultContentTypes,
            ContextView context,
            Function<Flux<SUBSCRIBE>, Publisher<RESULT>> subscribeWithResultTransform)
            throws IllegalArgumentException;

    default <SUBSCRIBE, RESULT> Disposable subscribeWithResult(
            ClientWebSocketOptions options,
            Class<? super SUBSCRIBE> subscribeType,
            Class<? super RESULT> resultType,
            String subscribeContentType,
            String resultContentType,
            ContextView context,
            Function<Flux<SUBSCRIBE>, Publisher<RESULT>> subscribeWithResultTransform)
            throws IllegalArgumentException
    {
        return subscribeWithResult(options, subscribeType, resultType, List.of(subscribeContentType), List.of(resultContentType), context, subscribeWithResultTransform);
    }

    default <SUBSCRIBE, RESULT> Disposable subscribeWithResult(
            ClientWebSocketOptions options,
            Class<? super SUBSCRIBE> subscribeType,
            Class<? super RESULT> resultType,
            ContextView context,
            Function<Flux<SUBSCRIBE>, Publisher<RESULT>> subscribeWithResultTransform)
            throws IllegalArgumentException
    {
        return subscribeWithResult(options, subscribeType, resultType, Collections.emptyList(), Collections.emptyList(), context, subscribeWithResultTransform);
    }
}
