package com.exoreaction.xorcery.service.eventstore;

import com.eventstore.dbclient.*;
import com.exoreaction.xorcery.configuration.Configuration;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Disabled
public class EventStoreIT {

    @Test
    public void testReadBackwards() throws IOException, ParseError, ExecutionException, InterruptedException, TimeoutException {
        Configuration configuration = Configuration.Builder.loadTest(null).build();

        String connectionString = configuration.getString("eventstore.url").orElseThrow();

        EventStoreDBClientSettings settings = EventStoreDBConnectionString.parse(connectionString);
        EventStoreDBClient client = EventStoreDBClient.create(settings);

        List<ResolvedEvent> lastEvent = client.readStream("development-default-forum", 1, ReadStreamOptions.get().backwards().fromEnd())
                .get(10, TimeUnit.SECONDS).getEvents();

        for (ResolvedEvent resolvedEvent : lastEvent) {
            System.out.println(resolvedEvent.getEvent().getStreamRevision().getValueUnsigned());
        }
    }
}
