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
package dev.xorcery.eventstore.streams;


import com.eventstore.dbclient.EventStoreDBClientSettings;
import com.eventstore.dbclient.EventStoreDBConnectionString;
import com.eventstore.dbclient.StreamMetadata;
import dev.xorcery.collections.Element;
import dev.xorcery.configuration.Configuration;
import dev.xorcery.configuration.InstanceConfiguration;
import dev.xorcery.domainevents.api.DomainEventMetadata;
import dev.xorcery.eventstore.client.api.EventStoreClient;
import dev.xorcery.jsonapi.service.ServiceResourceObject;
import dev.xorcery.jsonapi.service.ServiceResourceObjects;
import dev.xorcery.metadata.Metadata;
import dev.xorcery.metadata.WithMetadata;
import dev.xorcery.reactivestreams.api.ContextViewElement;
import dev.xorcery.reactivestreams.api.MetadataByteBuffer;
import dev.xorcery.reactivestreams.api.ReactiveStreamsContext;
import dev.xorcery.reactivestreams.api.server.ServerWebSocketStreams;
import io.opentelemetry.api.OpenTelemetry;
import jakarta.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.util.UUID;

import static dev.xorcery.collections.Element.missing;
import static dev.xorcery.reactivestreams.api.ReactiveStreamsContext.streamId;

@Service(name = "eventstore.api")
@RunLevel(6)
public class EventStoreStreamsService
        implements PreDestroy {
    private final Disposable publisher;
    private final Disposable subscriber;
    private final EventStoreClient eventStoreClient;
    private final Logger logger;

    @Inject
    public EventStoreStreamsService(ServiceResourceObjects serviceResourceObjects,
                                    Configuration configuration,
                                    ServerWebSocketStreams reactiveStreams,
                                    EventStoreClient.Factory eventStoreClientFactory,
                                    OpenTelemetry openTelemetry,
                                    Logger logger) {
        this.logger = logger;

        ServiceResourceObject sro = new ServiceResourceObject.Builder(InstanceConfiguration.get(configuration), "eventstore")
                .api("self", "api/eventstore")
                .build();
        serviceResourceObjects.add(sro);

        EventStoreDBClientSettings settings = EventStoreDBConnectionString.parseOrThrow(configuration.getString("eventstore.uri").orElseThrow(Element.missing("eventstore.uri")));
        eventStoreClient = eventStoreClientFactory.create(settings);

        // Test connection
        StreamMetadata allMetadata = eventStoreClient.getStreamMetadata("$all").join();
        logger.info("$all stream metadata:" + allMetadata.toString());

        // Read
        publisher = reactiveStreams.publisher("api/eventstore/{streamId}", MetadataByteBuffer.class,
                Flux.from(eventStoreClient.readStream())
//                        .subscribeOn(Schedulers.single(), true)
//                        .publishOn(Schedulers.newSingle(new NamedThreadFactory("EventStore")), 8192)
                        .contextWrite(context ->
                        {
                            ContextViewElement cv = new ContextViewElement(context);
                            logger.info("Publishing " + cv.get(streamId).orElseThrow(missing(streamId)) +
                                    cv.getLong(ReactiveStreamsContext.streamPosition).map(pos -> String.format(" from position %d", pos)).orElse(""));
                            return context;
                        }));
//                        .doOnRequest(System.out::println));

        // Write
        subscriber = reactiveStreams.subscriberWithResult(
                "api/eventstore/{streamId}",
                MetadataByteBuffer.class,
                Metadata.class,
                flux -> flux
                        .transformDeferredContextual(eventStoreClient.appendStream(this::eventId, this::eventType, options -> {
                        }))
                        .map(WithMetadata::metadata));
    }

    @Override
    public void preDestroy() {
        publisher.dispose();
        subscriber.dispose();
    }

    private UUID eventId(MetadataByteBuffer metadata) {
        try {
            return metadata.metadata().getString(DomainEventMetadata.correlationId).map(UUID::fromString).orElse(null);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String eventType(MetadataByteBuffer metadata) {
        return metadata.metadata().getString(DomainEventMetadata.commandName).orElse(null);
    }
}
