package com.exoreaction.reactiveservices.service.eventstore.disruptor;

import com.eventstore.dbclient.EventData;
import com.eventstore.dbclient.EventStoreDBClient;
import com.exoreaction.reactiveservices.disruptor.Event;
import com.exoreaction.reactiveservices.disruptor.EventWithResult;
import com.exoreaction.reactiveservices.disruptor.Metadata;
import com.exoreaction.reactiveservices.disruptor.StandardMetadata;
import com.exoreaction.reactiveservices.disruptor.handlers.DefaultEventHandler;
import com.exoreaction.reactiveservices.service.domainevents.api.DomainEventMetadata;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ReactiveEventStreams;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.UUID;

/**
 * @author rickardoberg
 * @since 18/04/2022
 */

public class EventStoreDomainEventEventHandler
        implements DefaultEventHandler<Event<EventWithResult<ByteBuffer, Metadata>>> {
    private final EventStoreDBClient client;
    private ReactiveEventStreams.Subscription subscription;
    private UUID eventId;
    private String eventType;

    public EventStoreDomainEventEventHandler(EventStoreDBClient client, ReactiveEventStreams.Subscription subscription) {

        this.client = client;
        this.subscription = subscription;
    }

    @Override
    public void onEvent(Event<EventWithResult<ByteBuffer, Metadata>> event, long sequence, boolean endOfBatch) throws Exception {
        Map<String, String> metadata = event.metadata.getMetadata();
        eventId = UUID.fromString(metadata.get(StandardMetadata.CORRELATION_ID));
        eventType = metadata.get(DomainEventMetadata.COMMAND_TYPE);
        EventData eventData = EventData.builderAsBinary(eventId, eventType, event.event.event().array())
                .metadataAsJson(event.metadata)
                .build();

        event.event.event().clear();
        String streamId = metadata.get(StandardMetadata.ENVIRONMENT) + "-" +
                metadata.get(StandardMetadata.TAG) + "-" +
                metadata.get(DomainEventMetadata.DOMAIN) + "-" +
                metadata.get(StandardMetadata.VERSION);

        client.appendToStream(streamId, eventData).whenComplete((wr, t) ->
        {
            if (t == null) {
                event.event.result().complete(new Metadata().add(DomainEventMetadata.POSITION, Long.toString(wr.getLogPosition().getCommitUnsigned())));
            } else {
                event.event.result().completeExceptionally(t);
            }

            subscription.request(1);
        });
    }
}
