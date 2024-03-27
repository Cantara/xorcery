package com.exoreaction.xorcery.reactivestreams.api.client;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public interface WebSocketStreamsClient {

    <PUBLISH> Flux<PUBLISH> publish(
            URI serverUri,
            WebSocketClientOptions options,
            Class<? super PUBLISH> publishType,
            Publisher<PUBLISH> publisher,
            String... publishContentTypes
    );

    <PUBLISH, RESULT> Flux<RESULT> publishWithResult(
            URI serverUri,
            WebSocketClientOptions options,
            Class<? super PUBLISH> publishType,
            Class<? super RESULT> resultType,
            Publisher<PUBLISH> publisher,
            Collection<String> messageContentTypes,
            Collection<String> resultContentTypes
    );

    default <PUBLISH, RESULT> Flux<RESULT> publishWithResult(
            URI serverUri,
            WebSocketClientOptions options,
            Class<? super PUBLISH> publishType,
            Class<? super RESULT> resultType,
            Publisher<PUBLISH> publisher,
            String messageContentType,
            String resultContentType
    )
    {
        return publishWithResult(serverUri,options, publishType, resultType, publisher, List.of(messageContentType), List.of(resultContentType));
    }

    default <PUBLISH, RESULT> Flux<RESULT> publishWithResult(
            URI serverUri,
            WebSocketClientOptions options,
            Class<? super PUBLISH> publishType,
            Class<? super RESULT> resultType,
            Publisher<PUBLISH> publisher
    ){
        return publishWithResult(serverUri, options, publishType, resultType, publisher, Collections.emptyList(), Collections.emptyList());
    }

    <SUBSCRIBE> Flux<SUBSCRIBE> subscribe(
            URI serverUri,
            WebSocketClientOptions options,
            Class<? super SUBSCRIBE> subscribeType,
            String... subscribeContentTypes
    );
}
