/*
 * Copyright © 2022 eXOReaction AS (rickard@exoreaction.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.exoreaction.xorcery.reactivestreams.server;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.reactivestreams.api.server.ServerShutdownStreamException;
import com.exoreaction.xorcery.reactivestreams.common.ExceptionObjectOutputStream;
import com.exoreaction.xorcery.reactivestreams.spi.MessageWriter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.eclipse.jetty.io.ByteBufferOutputStream2;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.io.EofException;
import org.eclipse.jetty.websocket.api.*;
import org.eclipse.jetty.websocket.api.exceptions.WebSocketTimeoutException;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayDeque;
import java.util.Base64;
import java.util.Deque;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * @author rickardoberg
 * @since 13/04/2022
 */
public class PublisherSubscriptionReactiveStream
        extends ServerReactiveStream
        implements WebSocketListener, Subscriber<Object> {

    protected final Logger logger;

    private final String streamName;
    protected volatile Session session;

    private final Function<Configuration, Publisher<Object>> publisherFactory;
    private Configuration publisherConfiguration;
    protected Subscription subscription;
    private boolean isComplete = false;

    private long requested;
    private final Deque<Object> queue = new ArrayDeque<>();
    private final ByteBufferOutputStream2 outputStream;

    protected final MessageWriter<Object> messageWriter;

    private final ObjectMapper objectMapper;
    protected final ByteBufferPool pool;
    protected final Marker marker;

    private boolean redundancyNotificationIssued = false;

    private long outstandingRequestAmount;

    public PublisherSubscriptionReactiveStream(String streamName,
                                               Function<Configuration, Publisher<Object>> publisherFactory,
                                               MessageWriter<Object> messageWriter,
                                               ObjectMapper objectMapper,
                                               ByteBufferPool pool,
                                               Logger logger) {
        this.streamName = streamName;
        this.publisherFactory = publisherFactory;
        this.messageWriter = messageWriter;
        this.objectMapper = objectMapper;
        this.pool = pool;
        marker = MarkerManager.getMarker(streamName);

        outputStream = new ByteBufferOutputStream2(pool, true);
        this.logger = logger;
    }

    // Subscriber
    @Override
    public synchronized void onSubscribe(Subscription subscription) {

        if (logger.isTraceEnabled())
            logger.trace(marker, "onSubscribe");

        this.subscription = subscription;

        if (outstandingRequestAmount > 0) {
            subscription.request(outstandingRequestAmount);
            outstandingRequestAmount = 0;
        }
    }

    @Override
    public synchronized void onNext(Object item) {
        if (logger.isTraceEnabled())
            logger.trace(marker, "onNext {}", item.toString());

        if (session.isOpen() && requested > 0) {
            send(item);
            requested--;
        } else {
            queue.add(item);
        }
    }

    // WebSocket
    @Override
    public synchronized void onWebSocketConnect(Session session) {
        if (logger.isTraceEnabled())
            logger.trace(marker, "onWebSocketConnect {}", session.getRemoteAddress().toString());

        this.session = session;
        session.getRemote().setBatchMode(BatchMode.ON);
    }

    @Override
    public synchronized void onWebSocketText(String message) {
        if (logger.isTraceEnabled())
            logger.trace(marker, "onWebSocketText {}", message);

        if (publisherConfiguration == null) {
            // Read JSON parameters
            try {
                ObjectNode configurationJson = (ObjectNode) objectMapper.readTree(message);
                publisherConfiguration = new Configuration.Builder(configurationJson)
                        .with(addUpgradeRequestConfiguration(session.getUpgradeRequest()))
                        .build();
                Publisher<Object> publisher = publisherFactory.apply(publisherConfiguration);
                publisher.subscribe(this);
            } catch (JsonProcessingException e) {
                session.close(StatusCode.BAD_PAYLOAD, e.getMessage());
            }
        } else {
            // Handle async
            CompletableFuture.runAsync(() ->
            {
                long requestAmount = Long.parseLong(message);

                synchronized (this) {
                    if (subscription != null) {
                        if (requestAmount == Long.MIN_VALUE) {
                            logger.info(marker, "Received cancel on websocket " + streamName);
                            session.close(StatusCode.NORMAL, "cancelled");
                        } else {
                            if (logger.isDebugEnabled())
                                logger.debug(marker, "Received request:" + requestAmount);

                            Object item;
                            while (requestAmount > 0 && (item = queue.poll()) != null && session.isOpen()) {
                                send(item);
                                requestAmount--;
                            }

                            if (isComplete) {
                                if (queue.isEmpty() && requestAmount == 0) {
                                    // We're done
                                    session.close(StatusCode.NORMAL, "complete");
                                }
                            } else {
                                if (requestAmount > 0) {
                                    requested += requestAmount;
                                    subscription.request(requestAmount);
                                }
                            }

                        }
                    } else {
                        outstandingRequestAmount += requestAmount;
                    }
                }
            });
        }
    }

    @Override
    public synchronized void onWebSocketBinary(byte[] payload, int offset, int len) {
        if (logger.isTraceEnabled())
            logger.trace(marker, "onWebSocketBinary");

        if (!redundancyNotificationIssued) {
            logger.warn(marker, "Receiving redundant results from subscriber");
            redundancyNotificationIssued = true;
        }
    }

    @Override
    public synchronized void onWebSocketClose(int statusCode, String reason) {
        if (logger.isTraceEnabled())
            logger.trace(marker, "onWebSocketClose {} {}", statusCode, reason);

        if (subscription != null) {
            subscription.cancel();
            subscription = null;
        }
    }

    @Override
    public synchronized void onWebSocketError(Throwable cause) {
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

    protected void send(Object item) {
        if (logger.isTraceEnabled())
            logger.trace(marker, "send {}", item.getClass().toString());

        // Write event data
        try {
            writeItem(messageWriter, item, outputStream);

            // Send it
            ByteBuffer eventBuffer = outputStream.takeByteBuffer();

            session.getRemote().sendBytes(eventBuffer, new WriteCallback() {
                @Override
                public void writeFailed(Throwable x) {
                    logger.error(marker, "Could not send event", x);
                    session.close(StatusCode.SERVER_ERROR, x.getMessage());
                }

                @Override
                public void writeSuccess() {
                    CompletableFuture.runAsync(() ->
                    {
                        if (queue.isEmpty()) {
                            if (logger.isTraceEnabled())
                                logger.trace(marker, "flush");
                            try {
                                session.getRemote().flush();
                            } catch (IOException e) {
                                logger.error(marker, "Could not flush events", e);
                                session.close(StatusCode.SERVER_ERROR, e.getMessage());
                            }
                        }
                    });
                }
            });

        } catch (Throwable t) {
            logger.error(marker, "Could not send event", t);
            session.close(StatusCode.SERVER_ERROR, t.getMessage());
        }
    }

    protected void writeItem(MessageWriter<Object> messageWriter, Object item, ByteBufferOutputStream2 outputStream) throws IOException {
        messageWriter.writeTo(item, outputStream);
    }

    public synchronized void onError(Throwable throwable) {
        if (logger.isTraceEnabled())
            logger.trace(marker, "onError", throwable);

        if (throwable instanceof ServerShutdownStreamException) {
            session.close(StatusCode.SHUTDOWN, throwable.getMessage());
        } else {
            // Send exception
            // Client should receive exception and close session
            try {
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                ObjectOutputStream out = new ExceptionObjectOutputStream(bout);
                out.writeObject(throwable);
                out.close();
                String base64Throwable = Base64.getEncoder().encodeToString(bout.toByteArray());
                session.getRemote().sendString(base64Throwable);
                session.getRemote().flush();
            } catch (IOException e) {
                logger.error(marker, "Could not send exception", e);
            }
        }
    }

    public synchronized void onComplete() {
        if (logger.isTraceEnabled())
            logger.trace(marker, "onComplete");

        if (queue.isEmpty()) {
            if (session.isOpen()) {
                session.close(StatusCode.NORMAL, "complete");
            }
        } else {
            // Wait for requests to drain the remaining items
            isComplete = true;
        }
    }
}
