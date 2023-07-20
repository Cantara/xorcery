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
package com.exoreaction.xorcery.reactivestreams.api.client;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.reactivestreams.spi.MessageReader;
import com.exoreaction.xorcery.reactivestreams.spi.MessageWriter;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public interface ReactiveStreamsClient {
    CompletableFuture<Void> publish(URI serverUri,
                                    String subscriberStreamName,
                                    Supplier<Configuration> subscriberServerConfiguration,

                                    Publisher<?> publisher,
                                    Class<? extends Publisher<?>> publisherType,
                                    ClientConfiguration publisherClientConfiguration);

    CompletableFuture<Void> publish(URI serverUri,
                                    String subscriberStreamName,
                                    Supplier<Configuration> subscriberServerConfiguration,

                                    Publisher<?> publisher,
                                    MessageWriter<?> messageWriter,
                                    MessageReader<?> messageReader,
                                    ClientConfiguration publisherClientConfiguration);

    CompletableFuture<Void> subscribe(URI serverUri,
                                      String publisherStreamName,
                                      Supplier<Configuration> publisherServerConfiguration,

                                      Subscriber<?> subscriber,
                                      Class<? extends Subscriber<?>> subscriberType,
                                      ClientConfiguration subscriberClientConfiguration);

    CompletableFuture<Void> subscribe(URI serverUri,
                                      String publisherStreamName,
                                      Supplier<Configuration> publisherServerConfiguration,

                                      Subscriber<?> subscriber,
                                      MessageReader<?> messageReader,
                                      MessageWriter<?> messageWriter,
                                      ClientConfiguration subscriberClientConfiguration);
}
