package com.exoreaction.reactiveservices.service.mapdbdomainevents.disruptor;

import com.exoreaction.reactiveservices.disruptor.Event;
import com.exoreaction.reactiveservices.disruptor.EventWithResult;
import com.exoreaction.reactiveservices.disruptor.Metadata;
import com.exoreaction.reactiveservices.disruptor.handlers.DefaultEventHandler;
import com.exoreaction.reactiveservices.service.domainevents.api.DomainEvents;
import com.exoreaction.reactiveservices.service.mapdatabase.MapDatabaseService;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ReactiveEventStreams;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

import java.io.StringReader;
import java.util.Map;

/**
 * @author rickardoberg
 * @since 18/04/2022
 */

public class MapDbDomainEventEventHandler
    implements DefaultEventHandler<Event<EventWithResult<DomainEvents, Metadata>>>
{
    private final MapDatabaseService mapDatabaseService;
    private ReactiveEventStreams.Subscription subscription;
    private final ObjectMapper objectMapper;
    long version = 0;

    public MapDbDomainEventEventHandler(MapDatabaseService mapDatabaseService,
                                        ReactiveEventStreams.Subscription subscription,
                                        ObjectMapper objectMapper) {
        this.mapDatabaseService = mapDatabaseService;
        this.subscription = subscription;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onEvent(Event<EventWithResult<DomainEvents, Metadata>> event, long sequence, boolean endOfBatch ) throws Exception
    {
        String jsonString = objectMapper.writeValueAsString(event.event.event());

        JsonObject eventsJson = Json.createReader(new StringReader(jsonString)).readObject();

        eventsJson.getJsonArray("events").stream().map(JsonObject.class::cast).forEach( jsonObject ->
        {
            for (Map.Entry<String, JsonValue> entry : jsonObject.entrySet()) {
                mapDatabaseService.put(entry.getKey(), entry.getValue().toString());
            }
        });

        version++;
        Metadata result = new Metadata();
        result.add("version", Long.toString(version));
        event.event.result().complete(result);

        subscription.request(1);
    }
}
