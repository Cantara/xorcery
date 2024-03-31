package com.exoreaction.xorcery.eventstore.client;

import com.eventstore.dbclient.EventStoreDBClient;
import com.eventstore.dbclient.ReadResult;
import com.eventstore.dbclient.ReadStreamOptions;
import com.eventstore.dbclient.StreamNotFoundException;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.util.context.Context;
import reactor.util.context.ContextView;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

import static com.exoreaction.xorcery.lang.Exceptions.unwrap;

public class LastPositionContext<T>
        implements BiFunction<Flux<T>, ContextView, Publisher<T>> {
    private final EventStoreDBClient client;
    private final String streamName;

    public LastPositionContext(EventStoreDBClient client, String streamName) {
        this.client = client;
        this.streamName = streamName;
    }

    @Override
    public Publisher<T> apply(Flux<T> flux, ContextView contextView) {

        try {
            String name = Optional.ofNullable(streamName).orElseGet(()->contextView.getOrDefault(EventStoreContext.streamId.name(), null));
            if (name == null)
                throw new IllegalArgumentException("No streamId name specified to find last streamPosition");
            ReadResult readResult = client.readStream(name, ReadStreamOptions.get().backwards().maxCount(1))
                    .orTimeout(10, TimeUnit.SECONDS).join();
            long lastStreamPosition = readResult.getLastStreamPosition();
            return flux.contextWrite(Context.of(EventStoreContext.streamPosition.name(), lastStreamPosition));
        } catch (Throwable e) {
            if (unwrap(e) instanceof StreamNotFoundException snf) {
                // There's no existing streamId so no streamPosition to report
                return flux;
            }

            // Break
            return Flux.error(e, true);
        }
    }
}
