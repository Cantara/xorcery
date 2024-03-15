package com.exoreaction.xorcery.reactivestreams.server.reactor;

import com.exoreaction.xorcery.opentelemetry.OpenTelemetryUnits;
import com.exoreaction.xorcery.reactivestreams.api.server.NotAuthorizedStreamException;
import com.exoreaction.xorcery.reactivestreams.api.server.ServerShutdownStreamException;
import com.exoreaction.xorcery.reactivestreams.spi.MessageWriter;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.semconv.SemanticAttributes;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.io.ByteBufferOutputStream2;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.io.EofException;
import org.eclipse.jetty.util.BlockingArrayQueue;
import org.eclipse.jetty.websocket.api.*;
import org.eclipse.jetty.websocket.api.exceptions.WebSocketTimeoutException;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.util.context.Context;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.exoreaction.xorcery.reactivestreams.util.ReactiveStreamsOpenTelemetry.*;
import static com.exoreaction.xorcery.reactivestreams.util.ReactiveStreamsOpenTelemetry.PUBLISHER_FLUSH_COUNT;

public class PublisherSubscriptionWebSocketStream
    implements CoreSubscriber<Object>, WebSocketListener
{
    private final String path;
    private final Publisher<Object> publisher;
    private final Map<String, String> publisherConfiguration;
    private final MessageWriter<Object> messageWriter;

    private final ByteBufferPool byteBufferPool;
    private final Logger logger;
    private final Marker marker;

    private volatile long outstandingRequestAmount;
    protected volatile Subscription subscription;
    protected volatile Session session;

    private AtomicBoolean isComplete = new AtomicBoolean();
    private volatile boolean redundancyNotificationIssued = false;

    private final BlockingArrayQueue<Object> sendQueue = new BlockingArrayQueue<>(4096, 1024);
    private final AtomicBoolean isDraining = new AtomicBoolean();
    private final Lock drainLock = new ReentrantLock();

    private final ByteBufferOutputStream2 outputStream;

    private final Tracer tracer;
    private final Attributes attributes;
    private final LongHistogram sentBytes;
    private final LongHistogram requestsHistogram;
    private final LongHistogram flushHistogram;
    private final io.opentelemetry.context.Context context;
    private volatile Span span;

    public PublisherSubscriptionWebSocketStream(
            String path,
            Publisher<Object> publisher,
            Map<String, String> publisherConfiguration,
            MessageWriter<Object> messageWriter,
            ByteBufferPool byteBufferPool,
            Logger logger,
            Tracer tracer,
            Meter meter,
            io.opentelemetry.context.Context context) {
        this.path = path;
        this.publisher = publisher;
        this.publisherConfiguration = publisherConfiguration;
        this.messageWriter = messageWriter;
        this.byteBufferPool = byteBufferPool;
        this.tracer = tracer;
        this.logger = logger;
        this.marker = MarkerManager.getMarker(path);

        this.outputStream = new ByteBufferOutputStream2(byteBufferPool, false);

        this.attributes = Attributes.builder()
                .put(SemanticAttributes.MESSAGING_DESTINATION_NAME, path)
                .put(SemanticAttributes.MESSAGING_SYSTEM, XORCERY_MESSAGING_SYSTEM)
                .build();
        this.sentBytes = meter.histogramBuilder(PUBLISHER_IO)
                .setUnit(OpenTelemetryUnits.BYTES).ofLongs().build();
        this.requestsHistogram = meter.histogramBuilder(PUBLISHER_REQUESTS)
                .setUnit("{request}").ofLongs().build();
        this.flushHistogram = meter.histogramBuilder(PUBLISHER_FLUSH_COUNT)
                .setUnit("{item}").ofLongs().build();

        this.context = context;
    }

    // Subscriber
    @Override
    public Context currentContext() {
        return Context.of(publisherConfiguration);
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        if (logger.isTraceEnabled())
            logger.trace(marker, "onSubscribe");

        this.subscription = subscription;

        if (outstandingRequestAmount > 0) {
            subscription.request(outstandingRequestAmount);
            outstandingRequestAmount = 0;
        }
    }

    @Override
    public void onNext(Object item) {
        if (logger.isTraceEnabled()) {
            logger.trace(marker, "onNext {}", item.toString());
        }

        if (!sendQueue.offer(item)) {
            logger.error(marker, "Could not put item on queue {}/{}", sendQueue.getCapacity(), sendQueue.getMaxCapacity());
        }

        if (!isDraining.get()) {
            CompletableFuture.runAsync(this::drainJob);
        }
    }

    @Override
    public void onError(Throwable throwable) {
        if (logger.isTraceEnabled())
            logger.trace(marker, "onError", throwable);

        if (throwable instanceof ServerShutdownStreamException) {
            session.close(StatusCode.SHUTDOWN, throwable.getMessage());
        } else {
            // Send error
            // Client should receive error and close session
            try {
                ObjectNode errorJson = getError(throwable);
                session.getRemote().sendString(errorJson.toPrettyString());
                session.getRemote().flush();
            } catch (IOException e) {
                logger.error(marker, "Could not send exception", e);
            }
        }
    }

    @Override
    public void onComplete() {
        if (logger.isTraceEnabled())
            logger.trace(marker, "onComplete");

        isComplete.set(true);
        if (!isDraining.get()) {
            CompletableFuture.runAsync(this::drainJob);
        }
    }

    // WebSocketListener
    @Override
    public void onWebSocketConnect(Session session) {
        if (logger.isTraceEnabled())
            logger.trace(marker, "onWebSocketConnect {}", session.getRemoteAddress().toString());

        this.session = session;
        session.getRemote().setBatchMode(BatchMode.ON);

        span = tracer.spanBuilder( path+ " publisher")
                .setParent(context)
                .setSpanKind(SpanKind.PRODUCER)
                .setAllAttributes(attributes)
                .startSpan();

        try {
            publisher.subscribe(this);
            logger.debug(marker, "Connected to {}", session.getRemote().getRemoteAddress().toString());
        } catch (Throwable e) {
            onError(e);
        }
    }

    @Override
    public void onWebSocketText(String message) {
        if (logger.isTraceEnabled())
            logger.trace(marker, "onWebSocketText {}", message);

        long requestAmount = Long.parseLong(message);

        if (subscription != null) {
            if (requestAmount == Long.MIN_VALUE) {
                logger.info(marker, "Received cancel on websocket " + path);
                session.close(StatusCode.NORMAL, "cancelled");
            } else {
                if (logger.isTraceEnabled())
                    logger.trace(marker, "Received request:" + requestAmount);

                if (requestAmount > 0) {
                    requestsHistogram.record(requestAmount, attributes);
                    subscription.request(requestAmount);
                }
            }
        } else {
            outstandingRequestAmount += requestAmount;
        }
    }

    @Override
    public void onWebSocketBinary(byte[] payload, int offset, int len) {
        if (logger.isTraceEnabled())
            logger.trace(marker, "onWebSocketBinary");

        if (!redundancyNotificationIssued) {
            logger.warn(marker, "Receiving redundant results from subscriber");
            redundancyNotificationIssued = true;
        }
    }

    @Override
    public void onWebSocketError(Throwable cause) {
        if (logger.isTraceEnabled())
            logger.trace(marker, "onWebSocketError", cause);

        if ((cause instanceof ClosedChannelException ||
                cause instanceof WebSocketTimeoutException ||
                cause instanceof EofException)
                && subscription != null) {
            // Ignore
        } else {
            logger.error(marker, "Publisher websocket error", cause);
        }

    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        if (logger.isTraceEnabled())
            logger.trace(marker, "onWebSocketClose {} {}", statusCode, reason);

        if (subscription != null) {
            subscription.cancel();
            subscription = null;
            span.end();
        }
    }

    private void drainJob() {
        drainLock.lock();
        isDraining.set(true);

        try (Scope scope = span.makeCurrent()) {
            logger.trace(marker, "Start drain");
            int count;
            int itemsSent = 0;
            List<Object> items = new ArrayList<>(sendQueue.size());
            int totalSent = 0;
            Span sentSpan = null;
            while ((count = sendQueue.drainTo(items)) > 0) {

                sentSpan = tracer.spanBuilder(path + " publish")
                        .setSpanKind(SpanKind.PRODUCER)
                        .startSpan();

                for (Object item : items) {
                    send(item);

                    itemsSent++;
                    if (itemsSent == 1024) {
                        if (logger.isTraceEnabled())
                            logger.trace(marker, "flush {}", itemsSent);
                        session.getRemote().flush();
                        flushHistogram.record(itemsSent, attributes);
                        itemsSent = 0;
                    }
                }

                items.clear();
                totalSent += count;
            }

            if (itemsSent > 0) {
                if (logger.isTraceEnabled())
                    logger.trace(marker, "flush {}", itemsSent);
                session.getRemote().flush();
                flushHistogram.record(itemsSent, attributes);

                sentSpan.setAttribute(SemanticAttributes.MESSAGING_BATCH_MESSAGE_COUNT, totalSent);
                sentSpan.end();
            }

        } catch (Throwable t) {
            logger.error(marker, "Could not send event", t);
            if (session.isOpen()) {
                session.close(StatusCode.SERVER_ERROR, t.getMessage());
            }
        } finally {
            logger.trace(marker, "Stop drain");

            isDraining.set(false);
            drainLock.unlock();
        }

        if (!sendQueue.isEmpty()) {
            // Race conditions suck. Need to figure out a better way to handle this...
            drainJob();
        } else {
            if (isComplete.get() && session.isOpen()) {
                session.close(StatusCode.NORMAL, "complete");
            }
        }
    }

    protected void send(Object item) throws IOException {
//        if (logger.isTraceEnabled())
//            logger.trace(marker, "send {}", item.getClass().toString());

        // Write event data
        writeItem(messageWriter, item, outputStream);
        ByteBuffer eventBuffer = outputStream.takeByteBuffer();
        sentBytes.record(eventBuffer.limit(), attributes);
        session.getRemote().sendBytes(eventBuffer);
        byteBufferPool.release(eventBuffer);
    }

    protected void writeItem(MessageWriter<Object> messageWriter, Object item, ByteBufferOutputStream2 outputStream) throws IOException {
        messageWriter.writeTo(item, outputStream);
    }

    protected ObjectNode getError(Throwable throwable) {
        ObjectNode errorJson = JsonNodeFactory.instance.objectNode();

        if (throwable instanceof NotAuthorizedStreamException) {
            errorJson.set("status", errorJson.numberNode(HttpStatus.UNAUTHORIZED_401));
        } else {
            errorJson.set("status", errorJson.numberNode(HttpStatus.INTERNAL_SERVER_ERROR_500));
        }

        errorJson.set("reason", errorJson.textNode(throwable.getMessage()));

        StringWriter exceptionWriter = new StringWriter();
        try (PrintWriter out = new PrintWriter(exceptionWriter)) {
            throwable.printStackTrace(out);
        }
        errorJson.set("exception", errorJson.textNode(exceptionWriter.toString()));
        return errorJson;
    }

}
