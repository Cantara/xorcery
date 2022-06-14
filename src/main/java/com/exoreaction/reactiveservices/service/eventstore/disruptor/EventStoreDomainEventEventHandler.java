package com.exoreaction.reactiveservices.service.eventstore.disruptor;

import com.eventstore.dbclient.EventData;
import com.eventstore.dbclient.EventStoreDBClient;
import com.exoreaction.reactiveservices.cqrs.metadata.DeploymentMetadata;
import com.exoreaction.reactiveservices.cqrs.metadata.DomainEventMetadata;
import com.exoreaction.reactiveservices.cqrs.metadata.Metadata;
import com.exoreaction.reactiveservices.cqrs.metadata.RequestMetadata;
import com.exoreaction.reactiveservices.disruptor.*;
import com.exoreaction.reactiveservices.disruptor.handlers.DefaultEventHandler;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ReactiveEventStreams;
import org.apache.logging.log4j.LogManager;

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
        String eventType = domainEventMetadata.getCommandType();
        EventData eventData = EventData.builderAsBinary(eventId, eventType, event.event.event().array())
                .metadataAsJson(event.metadata)
                .build();

        event.event.event().clear();
        DeploymentMetadata deploymentMetadata = new DeploymentMetadata(event.metadata);
        String streamId = deploymentMetadata.getEnvironment() + "-" +
                deploymentMetadata.getTag() + "-" +
                domainEventMetadata.getDomain();

        System.out.println("Write metadata:"+new String(eventData.getUserMetadata()));
        System.out.println("Write data:"+new String(eventData.getEventData()));

        try {
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
        } catch (Throwable e) {
            LogManager.getLogger(getClass()).error("Could not append event to stream", e);
        }
    }
}
