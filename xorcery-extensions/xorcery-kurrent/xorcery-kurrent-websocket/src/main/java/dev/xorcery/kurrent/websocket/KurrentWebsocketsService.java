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
package dev.xorcery.kurrent.websocket;


import dev.xorcery.concurrent.NamedThreadFactory;
import dev.xorcery.configuration.Configuration;
import dev.xorcery.kurrent.client.api.KurrentClient;
import dev.xorcery.kurrent.client.api.KurrentClients;
import dev.xorcery.kurrent.websocket.spi.ReadTransformerProvider;
import dev.xorcery.kurrent.websocket.spi.WriteTransformerProvider;
import dev.xorcery.metadata.Metadata;
import dev.xorcery.metadata.WithMetadata;
import dev.xorcery.reactivestreams.api.ContextViewElement;
import dev.xorcery.reactivestreams.api.MetadataByteBuffer;
import dev.xorcery.reactivestreams.api.ReactiveStreamsContext;
import dev.xorcery.reactivestreams.api.server.ServerWebSocketStreams;
import io.opentelemetry.api.OpenTelemetry;
import jakarta.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.api.IterableProvider;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.UUID;

import static dev.xorcery.collections.Element.missing;
import static dev.xorcery.reactivestreams.api.ReactiveStreamsContext.streamId;

@Service(name = "kurrent.websockets")
@RunLevel(6)
public class KurrentWebsocketsService
        implements PreDestroy {
    private final Disposable publisher;
    private final Disposable subscriber;
    private final KurrentClient readClient;
    private final KurrentClient writeClient;
    private final Logger logger;

    @Inject
    public KurrentWebsocketsService(Configuration configuration,
                                    ServerWebSocketStreams reactiveStreams,
                                    KurrentClients kurrentClients,
                                    IterableProvider<ReadTransformerProvider> readTransformerProviders,
                                    IterableProvider<WriteTransformerProvider> writeTransformerProviders,
                                    OpenTelemetry openTelemetry,
                                    Logger logger) {
        this.logger = logger;

        KurrentWebsocketsConfiguration config = KurrentWebsocketsConfiguration.get(configuration);
        readClient = kurrentClients.getClient(config.readClient());
        writeClient = kurrentClients.getClient(config.writeClient());

        // Read
        Flux<MetadataByteBuffer> readFlux = Flux.from(readClient.readStream())
                .subscribeOn(Schedulers.boundedElastic(), true)
                .publishOn(Schedulers.newSingle(new NamedThreadFactory("Kurrent")), config.prefetch())
                .contextWrite(context ->
                {
                    ContextViewElement cv = new ContextViewElement(context);
                    logger.info("Reading " + cv.get(streamId).orElseThrow(missing(streamId)) +
                            cv.getLong(ReactiveStreamsContext.streamPosition).map(pos -> String.format(" from position %d", pos)).orElse(""));
                    return context;
                });
        for (ReadTransformerProvider readTransformerProvider : readTransformerProviders) {
            readFlux = readFlux.transformDeferredContextual(readTransformerProvider);
        }
        publisher = reactiveStreams.publisher("api/kurrent/{streamId}", MetadataByteBuffer.class, readFlux);

        // Write
        subscriber = reactiveStreams.subscriberWithResult(
                "api/kurrent/{streamId}",
                MetadataByteBuffer.class,
                Metadata.class,
                flux -> {
                    flux.contextWrite(context ->{
                        ContextViewElement cv = new ContextViewElement(context);
                        logger.info("Writing " + cv.get(streamId).orElseThrow(missing(streamId)));
                        return context;
                    });
                    for (WriteTransformerProvider writeTransformerProvider : writeTransformerProviders) {
                        flux = flux.transformDeferredContextual(writeTransformerProvider);
                    }
                    return flux.transformDeferredContextual(writeClient.appendStream(this::eventId, this::eventType, options -> {
                            }))
                            .map(WithMetadata::metadata);
                });
    }

    @Override
    public void preDestroy() {
        publisher.dispose();
        subscriber.dispose();
    }

    private UUID eventId(MetadataByteBuffer metadata) {
        try {
            return metadata.metadata().getString("correlationId").map(UUID::fromString).orElse(null);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String eventType(MetadataByteBuffer metadata) {
        return metadata.metadata().getString("commandName").orElse(null);
    }
}
