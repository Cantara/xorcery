package com.exoreaction.reactiveservices.service.mapdbdomainevents.disruptor;

import com.exoreaction.reactiveservices.disruptor.DefaultEventHandler;
import com.exoreaction.reactiveservices.disruptor.EventHolder;
import com.exoreaction.reactiveservices.service.mapdatabase.MapDatabaseService;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

import java.util.List;
import java.util.Map;

/**
 * @author rickardoberg
 * @since 18/04/2022
 */

public class MapDbDomainEventEventHandler
    implements DefaultEventHandler<EventHolder<JsonArray>>
{
    private final MapDatabaseService mapDatabaseService;

    public MapDbDomainEventEventHandler(MapDatabaseService mapDatabaseService) {
        this.mapDatabaseService = mapDatabaseService;
    }

    @Override
    public void onEvent( EventHolder<JsonArray> event, long sequence, boolean endOfBatch ) throws Exception
    {
        event.event.stream().map(JsonObject.class::cast).forEach( jsonObject ->
        {
            for (Map.Entry<String, JsonValue> entry : jsonObject.entrySet()) {
                mapDatabaseService.put(entry.getKey(), entry.getValue().toString());
            }
        });
    }
}
