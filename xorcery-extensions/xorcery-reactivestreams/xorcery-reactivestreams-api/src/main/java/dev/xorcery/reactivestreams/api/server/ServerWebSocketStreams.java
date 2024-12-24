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
package dev.xorcery.reactivestreams.api.server;

import org.reactivestreams.Publisher;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.util.function.Function;

public interface ServerWebSocketStreams {
    <PUBLISH> Disposable publisher(
            String path,
            Class<? super PUBLISH> publishType,
            Publisher<PUBLISH> publisher)
            throws IllegalArgumentException;

    <PUBLISH, RESULT> Disposable publisherWithResult(
            String path,
            Class<? super PUBLISH> publishType,
            Class<? super RESULT> resultType,
            Function<Flux<RESULT>, Publisher<PUBLISH>> publisherWithResultTransform)
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
