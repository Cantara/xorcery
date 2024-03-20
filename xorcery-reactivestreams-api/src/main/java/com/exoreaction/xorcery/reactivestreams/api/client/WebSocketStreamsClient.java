package com.exoreaction.xorcery.reactivestreams.api.client;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import java.net.URI;

public interface WebSocketStreamsClient {

    <PUBLISH> Flux<PUBLISH> publish(
            URI serverUri,
            String contentType,
            Class<PUBLISH> publishType,
            WebSocketClientOptions options,
            Publisher<PUBLISH> publisher
    );

    <PUBLISH, RESULT> Flux<RESULT> publishWithResult(
            URI serverUri,
            String contentType,
            Class<PUBLISH> publishType,
            Class<RESULT> resultType,
            WebSocketClientOptions options,
            Publisher<PUBLISH> publisher
    );

    <SUBSCRIBE> Flux<SUBSCRIBE> subscribe(
            URI serverUri,
            String contentType,
            Class<SUBSCRIBE> subscribeType,
            WebSocketClientOptions options
    );
}
