package com.exoreaction.xorcery.service.eventstore.disruptor;

import com.eventstore.dbclient.EventData;
import com.eventstore.dbclient.EventDataBuilder;
import com.eventstore.dbclient.EventStoreDBClient;
import com.exoreaction.xorcery.cqrs.metadata.Metadata;
import com.exoreaction.xorcery.service.reactivestreams.api.WithMetadata;
import com.exoreaction.xorcery.disruptor.handlers.DefaultEventHandler;
import com.exoreaction.xorcery.service.domainevents.api.DomainEventMetadata;
import com.exoreaction.xorcery.service.eventstore.api.EventStoreMetadata;
import com.exoreaction.xorcery.service.reactivestreams.api.Subscription;
import com.exoreaction.xorcery.service.reactivestreams.api.WithResult;
import com.lmax.disruptor.EventHandler;
import org.apache.logging.log4j.LogManager;

import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.Flow;

/**
 * @author rickardoberg
 * @since 18/04/2022
 */

public class EventStoreDomainEventEventHandler
        implements EventHandler<WithResult<WithMetadata<ByteBuffer>, Metadata>> {
    private final EventStoreDBClient client;
    private Flow.Subscription subscription;

    public EventStoreDomainEventEventHandler(EventStoreDBClient client, Flow.Subscription subscription) {

        this.client = client;
        this.subscription = subscription;
    }

    @Override
    public void onEvent(WithResult<WithMetadata<ByteBuffer>, Metadata> event, long sequence, boolean endOfBatch) throws Exception {
        EventStoreMetadata emd = new EventStoreMetadata(event.event().metadata());
        UUID eventId = emd.getCorrelationId().map(UUID::fromString).orElseGet(UUID::randomUUID);
        DomainEventMetadata domainEventMetadata = new DomainEventMetadata(event.event().metadata());
        String eventType = domainEventMetadata.getCommandType();
        EventData eventData = EventDataBuilder.json(eventId, eventType, event.event().event().array())
                .metadataAsJson(event.event().metadata().metadata())
                .build();

        event.event().event().clear();
        String streamId = emd.getEnvironment() + "-" +
                emd.getTag() + "-" +
                domainEventMetadata.getDomain();

        System.out.println("Write metadata:" + new String(eventData.getUserMetadata()));
        System.out.println("Write data:" + new String(eventData.getEventData()));

        try {
            client.appendToStream(streamId, eventData).whenComplete((wr, t) ->
            {
                if (t == null) {
                    event.result().complete(new EventStoreMetadata.Builder(new Metadata.Builder())
                            .streamId(streamId)
                            .revision(wr.getNextExpectedRevision().getValueUnsigned())
                            .build().metadata());
                } else {
                    event.result().completeExceptionally(t);
                }

                subscription.request(1);
            });
        } catch (Throwable e) {
            LogManager.getLogger(getClass()).error("Could not append event to stream", e);
        }
    }
}
