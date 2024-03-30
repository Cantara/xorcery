package com.exoreaction.xorcery.eventstore.reactor;

import com.eventstore.dbclient.EventStoreDBClient;
import com.eventstore.dbclient.ReadResult;
import com.eventstore.dbclient.ReadStreamOptions;
import com.eventstore.dbclient.StreamNotFoundException;
import reactor.util.context.Context;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.exoreaction.xorcery.lang.Exceptions.unwrap;

public class LastPositionContext
        implements Function<Context, Context> {
    private final EventStoreDBClient client;
    private final String streamName;

    public LastPositionContext(EventStoreDBClient client, String streamName) {
        this.client = client;
        this.streamName = streamName;
    }

    @Override
    public Context apply(Context context) {
        try {
            ReadResult readResult = client.readStream(streamName, ReadStreamOptions.get().backwards().maxCount(1))
                    .orTimeout(10, TimeUnit.SECONDS).join();
            long lastStreamPosition = readResult.getLastStreamPosition();
            return context.put("position", lastStreamPosition);
        } catch (Exception e) {
            if (unwrap(e) instanceof StreamNotFoundException snf) {
                return context.put("position", 0);
            }
        }
        return context;
    }
}
