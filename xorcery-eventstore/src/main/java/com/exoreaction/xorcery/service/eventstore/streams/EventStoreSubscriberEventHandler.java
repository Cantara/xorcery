package com.exoreaction.xorcery.service.eventstore.streams;

import com.eventstore.dbclient.EventData;
import com.eventstore.dbclient.EventDataBuilder;
import com.eventstore.dbclient.EventStoreDBClient;
import com.exoreaction.xorcery.service.eventstore.api.EventStoreMetadata;
import com.exoreaction.xorcery.service.reactivestreams.api.WithMetadata;
import com.lmax.disruptor.EventHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Flow;

/**
 * @author rickardoberg
 * @since 18/04/2022
 */

public class EventStoreSubscriberEventHandler
        implements EventHandler<WithMetadata<ByteBuffer>> {
    private final EventStoreDBClient client;
    private final Optional<String> streamId;
    private final Logger logger = LogManager.getLogger(getClass());
    private final Flow.Subscription subscription;

    public EventStoreSubscriberEventHandler(EventStoreDBClient client, Flow.Subscription subscription, Optional<String> streamId) {

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
                .metadataAsJson(event.metadata().metadata())
                .build();

        event.event().clear();
        String streamId = this.streamId.orElseGet(emd::streamId);

        logger.debug("Write metadata:" + new String(eventData.getUserMetadata()) + "\nWrite data:" + new String(eventData.getEventData()));

        try {
            client.appendToStream(streamId, eventData).whenComplete((wr, t) ->
            {
                if (t != null)
                {
                    LogManager.getLogger(getClass()).error("Could not append event to stream", t);
                    subscription.cancel();
                } else
                {
                    subscription.request(1);
                }
            });
        } catch (Throwable e) {
            LogManager.getLogger(getClass()).error("Could not append event to stream", e);
        }
    }
}
