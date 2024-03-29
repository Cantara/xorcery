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
package com.exoreaction.xorcery.reactivestreams.api.server;

import com.exoreaction.xorcery.configuration.Configuration;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public interface ReactiveStreamsServer {
    CompletableFuture<Void> publisher(String streamName,
                                      Function<Configuration, ? extends Publisher<?>> publisherFactory,
                                      Class<? extends Publisher<?>> publisherType);

    CompletableFuture<Void> subscriber(String streamName,
                                       Function<Configuration, Subscriber<?>> subscriberFactory,
                                       Class<? extends Subscriber<?>> subscriberType);
}
