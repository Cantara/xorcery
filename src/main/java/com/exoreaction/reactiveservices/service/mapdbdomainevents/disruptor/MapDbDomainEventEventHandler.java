package com.exoreaction.reactiveservices.service.mapdbdomainevents.disruptor;

import com.exoreaction.reactiveservices.disruptor.handlers.DefaultEventHandler;
import com.exoreaction.reactiveservices.disruptor.Event;
import com.exoreaction.reactiveservices.disruptor.Metadata;
import com.exoreaction.reactiveservices.service.mapdatabase.MapDatabaseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.jetty.websocket.api.Session;

import java.nio.ByteBuffer;
import java.util.Map;

/**
 * @author rickardoberg
 * @since 18/04/2022
 */

public class MapDbDomainEventEventHandler
    implements DefaultEventHandler<Event<JsonObject>>
{
    private final MapDatabaseService mapDatabaseService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private Session session;

    long version = 0;

    public MapDbDomainEventEventHandler(MapDatabaseService mapDatabaseService, Session session) {
        this.mapDatabaseService = mapDatabaseService;
        this.session = session;
    }

    @Override
    public void onEvent(Event<JsonObject> event, long sequence, boolean endOfBatch ) throws Exception
    {
        event.event.getJsonArray("events").stream().map(JsonObject.class::cast).forEach( jsonObject ->
        {
            for (Map.Entry<String, JsonValue> entry : jsonObject.entrySet()) {
                mapDatabaseService.put(entry.getKey(), entry.getValue().toString());
            }
        });

        version++;
        Metadata result = new Metadata();
        result.add("version", Long.toString(version));
        session.getRemote().sendBytes(ByteBuffer.wrap( objectMapper.writeValueAsBytes( result )));
    }
}
