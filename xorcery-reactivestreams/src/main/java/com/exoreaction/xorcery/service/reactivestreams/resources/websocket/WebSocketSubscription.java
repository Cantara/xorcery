package com.exoreaction.xorcery.service.reactivestreams.resources.websocket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WriteCallback;

import java.io.IOException;
import java.util.concurrent.Flow;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

class WebSocketSubscription implements Flow.Subscription, Runnable {

    private static final AtomicLong nextInternalSubscriptionId = new AtomicLong(1);

    private static final Logger logger = LogManager.getLogger(WebSocketSubscription.class);

    private final AtomicLong requests = new AtomicLong();
    private final Session session;
    private final Marker marker;

    private final Thread thread = new Thread(this, "websocket-subscription-" + nextInternalSubscriptionId.getAndIncrement());
    private final Semaphore possibleBackPressure = new Semaphore(0);
    private final AtomicBoolean cancelled = new AtomicBoolean();

    public WebSocketSubscription(Session session, Marker marker) {
        this.session = session;
        this.marker = marker;
        thread.start();
    }

    @Override
    public void request(long n) {
        logger.trace(marker, "request() called: {}", n);
        if (cancelled.get()) {
            return;
        }
        requests.addAndGet(n);
        possibleBackPressure.release();
    }

    @Override
    public void cancel() {
        logger.trace(marker, "cancel() called");
        cancelled.set(true);
        requests.set(Long.MIN_VALUE);
        possibleBackPressure.release();
    }

    @Override
    public void run() {
        try {
            for (; ; ) {

                while (!possibleBackPressure.tryAcquire(5, TimeUnit.SECONDS)) {
                    if (!session.isOpen()) {
                        logger.info(marker, "Session closed (semaphore acquire timeout), subscription thread will terminate.");
                        return;
                    }
                }

                possibleBackPressure.drainPermits();

                if (!session.isOpen()) {
                    logger.info(marker, "Session closed, subscription thread will terminate.");
                    return;
                }

                final long rn = requests.getAndSet(0);
                if (rn == 0) {
                    if (cancelled.get()) {
                        logger.info(marker, "Subscription cancelled");
                        return; // cancel signal has already been sent across socket, so we can safely let this thread die
                    }
                    continue;
                }

                logger.trace(marker, "Sending request: {}", rn);
                session.getRemote().sendString(Long.toString(rn), new WriteCallback() {
                    @Override
                    public void writeFailed(Throwable x) {
                        logger.error(marker, "Could not send request {}", rn, x);
                    }

                    @Override
                    public void writeSuccess() {
                        logger.trace(marker, "Successfully sent request {}", rn);
                    }
                });

                try {
                    logger.trace(marker, "Flushing remote session...");
                    session.getRemote().flush();
                    logger.trace(marker, "Remote session flushed.");
                } catch (IOException e) {
                    logger.error(marker, "While flushing remote session", e);
                }
            }
        } catch (Throwable t) {
            logger.error(marker, "", t);
        } finally {
            logger.info(marker, "Subscription thread terminated!");
        }
    }
}
