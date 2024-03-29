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

import com.eventstore.dbclient.EventData;
import com.eventstore.dbclient.EventDataBuilder;
import com.eventstore.dbclient.EventStoreDBClient;
import com.exoreaction.xorcery.eventstore.api.EventStoreMetadata;
import com.exoreaction.xorcery.reactivestreams.api.WithMetadata;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.lmax.disruptor.EventHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reactivestreams.Subscription;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.UUID;

/**
 * @author rickardoberg
 * @since 18/04/2022
 */

public class EventStoreSubscriberEventHandler
        implements EventHandler<WithMetadata<ByteBuffer>> {
    private final EventStoreDBClient client;
    private final Optional<String> streamId;
    private final Logger logger = LogManager.getLogger(getClass());
    private final Subscription subscription;
    private final JsonMapper jsonMapper = new JsonMapper();

    public EventStoreSubscriberEventHandler(EventStoreDBClient client, Subscription subscription, Optional<String> streamId) {

        this.client = client;
        this.subscription = subscription;
        this.streamId = streamId;
    }

    @Override
    public void onEvent(WithMetadata<ByteBuffer> event, long sequence, boolean endOfBatch) throws Exception {
        EventStoreMetadata emd = new EventStoreMetadata(event.metadata());
        UUID eventId = emd.getCorrelationId().map(UUID::fromString).orElseGet(UUID::randomUUID);
        String eventType = emd.eventType();
        EventData eventData = EventDataBuilder.json(eventId, eventType, event.event().array())
                .metadataAsBytes(jsonMapper.writeValueAsBytes(event.metadata().metadata()))
                .build();

        event.event().clear();
        String streamId = this.streamId.orElseGet(emd::streamId);

        logger.debug("Write metadata:" + new String(eventData.getUserMetadata()) + "\nWrite data:" + new String(eventData.getEventData()));

        try {
            client.appendToStream(streamId, eventData).whenComplete((wr, t) ->
            {
                if (t != null) {
                    LogManager.getLogger(getClass()).error("Could not append event to stream", t);
                    subscription.cancel();
                } else {
                    subscription.request(1);
                }
            });
        } catch (Throwable e) {
            LogManager.getLogger(getClass()).error("Could not append event to stream", e);
        }
    }
}
