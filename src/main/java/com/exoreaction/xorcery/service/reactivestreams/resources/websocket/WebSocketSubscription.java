package com.exoreaction.xorcery.service.reactivestreams.resources.websocket;

import com.exoreaction.xorcery.service.reactivestreams.api.Subscription;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WriteCallback;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

class WebSocketSubscription implements Flow.Subscription {

    private static final Logger logger = LogManager.getLogger(WebSocketSubscription.class);

    private final AtomicLong requests;
    private final AtomicReference<CompletableFuture<Void>> sendRequests;
    private final Session session;
    private final Marker marker;

    public WebSocketSubscription(Session session, Marker marker) {
        this.session = session;
        this.marker = marker;
        requests = new AtomicLong();
        sendRequests = new AtomicReference<>();
    }

    @Override
    public synchronized void request(long n) {
        if (!session.isOpen())
            return;

        requests.addAndGet(n);

        if (sendRequests.get() == null) {
            sendRequests.set(new CompletableFuture<>());
            sendRequests.get().whenComplete((v, t) ->
            {
                synchronized (this) {
                    sendRequests.set(null);
                    long rn = requests.getAndSet(0);
                    if (rn > 0) {
                        request(rn);
                    }
                }
            });

            long rn = requests.getAndSet(0);
            session.getRemote().sendString(Long.toString(rn), new WriteCallback() {
                @Override
                public void writeFailed(Throwable x) {
                    synchronized (this) {
                        logger.error(marker, "Could not send request", x);
                        sendRequests.get().completeExceptionally(x);
                    }
                }

                @Override
                public void writeSuccess() {
                    synchronized (this) {
                        sendRequests.get().complete(null);
                        logger.debug(marker, "Sent request {}", rn);
                    }
                }
            });
            try {
                session.getRemote().flush();
            } catch (IOException e) {
                CompletableFuture<Void> completableFuture = sendRequests.get();
                if (completableFuture != null)
                    completableFuture.completeExceptionally(e);
            }
        }
    }

    @Override
    public synchronized void cancel() {
        requests.set(0);
        request(Long.MIN_VALUE);
    }
}
