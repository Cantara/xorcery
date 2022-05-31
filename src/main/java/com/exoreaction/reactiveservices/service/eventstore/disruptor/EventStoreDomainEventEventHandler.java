package com.exoreaction.reactiveservices.service.eventstore.disruptor;

import com.eventstore.dbclient.EventData;
import com.eventstore.dbclient.EventStoreDBClient;
import com.exoreaction.reactiveservices.disruptor.*;
import com.exoreaction.reactiveservices.disruptor.handlers.DefaultEventHandler;
import com.exoreaction.reactiveservices.service.domainevents.api.DomainEventMetadata;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ReactiveEventStreams;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * @author rickardoberg
 * @since 18/04/2022
 */

public class EventStoreDomainEventEventHandler
        implements DefaultEventHandler<Event<EventWithResult<ByteBuffer, Metadata>>> {
    private final EventStoreDBClient client;
    private ReactiveEventStreams.Subscription subscription;

    public EventStoreDomainEventEventHandler(EventStoreDBClient client, ReactiveEventStreams.Subscription subscription) {

        this.client = client;
        this.subscription = subscription;
    }

    @Override
    public void onEvent(Event<EventWithResult<ByteBuffer, Metadata>> event, long sequence, boolean endOfBatch) throws Exception {
        RequestMetadata requestMetadata = new RequestMetadata(event.metadata);
        UUID eventId = requestMetadata.correlationId().map(UUID::fromString).orElseGet(UUID::randomUUID);
        DomainEventMetadata domainEventMetadata = new DomainEventMetadata(event.metadata);
        String eventType = domainEventMetadata.commandType();
        EventData eventData = EventData.builderAsBinary(eventId, eventType, event.event.event().array())
                .metadataAsJson(event.metadata)
                .build();

        event.event.event().clear();
        DeploymentMetadata deploymentMetadata = new DeploymentMetadata(event.metadata);
        String streamId = deploymentMetadata.environment() + "-" +
                deploymentMetadata.tag() + "-" +
                domainEventMetadata.domain() + "-" +
                deploymentMetadata.version();

        client.appendToStream(streamId, eventData).whenComplete((wr, t) ->
        {
            if (t == null) {
                event.event.result().complete(new Metadata.Builder()
                        .add("position", Long.toString(wr.getLogPosition().getCommitUnsigned()))
                        .build());
            } else {
                event.event.result().completeExceptionally(t);
            }


            subscription.request(1);
        });
    }
}
