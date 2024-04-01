package com.exoreaction.xorcery.eventstore.client;

import com.eventstore.dbclient.EventStoreDBClient;
import com.eventstore.dbclient.ReadResult;
import com.eventstore.dbclient.ReadStreamOptions;
import com.eventstore.dbclient.StreamNotFoundException;
import com.exoreaction.xorcery.eventstore.client.api.EventStoreContext;
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

    public LastPositionContext(EventStoreDBClient client) {
        this.client = client;
    }

    @Override
    public Publisher<T> apply(Flux<T> flux, ContextView contextView) {

        try {
            String name = contextView.get(EventStoreContext.streamId.name());
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
