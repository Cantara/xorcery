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
package com.exoreaction.xorcery.eventstore.streams;

import com.eventstore.dbclient.EventStoreDBClient;
import com.eventstore.dbclient.ResolvedEvent;
import com.eventstore.dbclient.SubscribeToStreamOptions;
import com.eventstore.dbclient.SubscriptionListener;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.eventstore.api.EventStoreMetadata;
import com.exoreaction.xorcery.eventstore.resources.EventStoreParameters;
import com.exoreaction.xorcery.metadata.Metadata;
import com.exoreaction.xorcery.reactivestreams.api.MetadataByteBuffer;
import com.exoreaction.xorcery.reactivestreams.api.WithMetadata;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

public class EventStorePublisher
        implements Publisher<MetadataByteBuffer> {
    private static final Logger logger = LogManager.getLogger(EventStorePublisher.class);

    private final EventStoreDBClient client;
    private final ObjectMapper objectMapper;
    private Configuration publisherConfiguration;

    public EventStorePublisher(EventStoreDBClient client, ObjectMapper objectMapper, Configuration publisherConfiguration) {
        this.client = client;
        this.objectMapper = objectMapper;
        this.publisherConfiguration = publisherConfiguration;
    }

    @Override
    public void subscribe(Subscriber<? super MetadataByteBuffer> subscriber) {
        new EventsProcessor(subscriber);
    }

    private class EventsProcessor
            extends SubscriptionListener
            implements Subscription {

        private Subscriber<? super MetadataByteBuffer> subscriber;
        private com.eventstore.dbclient.Subscription subscription;

        public EventsProcessor(Subscriber<? super MetadataByteBuffer> subscriber) {
            try {
                this.subscriber = subscriber;

                EventStoreParameters parameters = objectMapper.treeToValue(publisherConfiguration.json(), EventStoreParameters.class);

                long from = publisherConfiguration.getLong("revision").orElse(parameters.from);

                SubscribeToStreamOptions subscribeToStreamOptions = from == 0 ?
                        SubscribeToStreamOptions.get().fromStart() :
                        SubscribeToStreamOptions.get().fromRevision(from);
                subscription = client.subscribeToStream(parameters.stream, this, subscribeToStreamOptions).get(10, TimeUnit.SECONDS);

                subscriber.onSubscribe(this);
            } catch (Throwable e) {
                logger.error("Could not subscribe to EventStore streamId", e);
                subscriber.onError(e);
            }
        }

        @Override
        public void request(long n) {
            // TODO Semaphore this one ?
        }

        @Override
        public void cancel() {
            subscription.stop();
            subscriber.onComplete();
        }

        public void onEvent(com.eventstore.dbclient.Subscription subscription, ResolvedEvent resolvedEvent) {
            try {
/*
                    logger.info(MarkerManager.getMarker(sro.serviceIdentifier().toString()), "Read metadata: " + new String(e.getEvent().getUserMetadata()));
                    logger.info(MarkerManager.getMarker(sro.serviceIdentifier().toString()), "Read event: " + new String(e.getEvent().getEventData()));
*/

                Metadata.Builder metadata = new Metadata.Builder((ObjectNode) objectMapper.readTree(resolvedEvent.getEvent().getUserMetadata()));

                // Put in ES metadata
                new EventStoreMetadata.Builder(metadata)
                        .streamId(resolvedEvent.getEvent().getStreamId())
                        .revision(resolvedEvent.getEvent().getRevision())
                        .contentType(resolvedEvent.getEvent().getContentType());

                subscriber.onNext(new MetadataByteBuffer(metadata.build(), ByteBuffer.wrap(resolvedEvent.getEvent().getEventData())));

            } catch (IOException ex) {
                subscription.stop();
                subscriber.onError(ex);
            }
        }

        @Override
        public void onCancelled(com.eventstore.dbclient.Subscription subscription, Throwable exception) {
            if (exception == null)
            {
                subscriber.onError(exception);
            } else
            {
                subscriber.onComplete();
            }
            super.onCancelled(subscription, exception);
        }
    }
}
