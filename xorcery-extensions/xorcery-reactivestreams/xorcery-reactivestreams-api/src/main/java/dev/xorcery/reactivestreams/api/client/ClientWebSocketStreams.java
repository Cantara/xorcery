/*
 * Copyright © 2022 eXOReaction AS (rickard@exoreaction.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.xorcery.reactivestreams.api.client;

import reactor.core.publisher.Flux;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Use these methods to create Flux instance for the various operations.
 * The methods without a specific server URI to connect to must be supplied via {@link reactor.util.context.ContextView}
 * under the key {@link ClientWebSocketStreamContext#serverUri} as a {@link URI} or {@link String}.
 */
public interface ClientWebSocketStreams {

    <PUBLISH> CompletableFuture<Void> publish(
            Flux<PUBLISH> publisher,
            URI serverURI,
            ClientWebSocketOptions options,
            Class<? super PUBLISH> publishType,
            String... publishContentTypes
    );

    <PUBLISH, RESULT> Flux<RESULT> publishWithResult(
            Flux<PUBLISH> publisher,
            URI serverURI,
            ClientWebSocketOptions options,
            Class<? super PUBLISH> publishType,
            Class<? super RESULT> resultType,
            Collection<String> messageContentTypes,
            Collection<String> resultContentTypes
    );

    default <PUBLISH, RESULT> Flux<RESULT> publishWithResult(
            Flux<PUBLISH> publisher,
            URI serverURI,
            ClientWebSocketOptions options,
            Class<? super PUBLISH> publishType,
            Class<? super RESULT> resultType,
            String messageContentType,
            String resultContentType
    )
    {
        return publishWithResult(publisher, serverURI, options, publishType, resultType, List.of(messageContentType), List.of(resultContentType));
    }

    default <PUBLISH, RESULT> Flux<RESULT> publishWithResult(
            Flux<PUBLISH> publisher,
            URI serverURI,
            ClientWebSocketOptions options,
            Class<? super PUBLISH> publishType,
            Class<? super RESULT> resultType
    ){
        return publishWithResult(publisher, serverURI, options, publishType, resultType, Collections.emptyList(), Collections.emptyList());
    }

    <SUBSCRIBE> Flux<SUBSCRIBE> subscribe(
            URI serverUri,
            ClientWebSocketOptions options,
            Class<? super SUBSCRIBE> subscribeType,
            String... subscribeContentTypes
    );
}
