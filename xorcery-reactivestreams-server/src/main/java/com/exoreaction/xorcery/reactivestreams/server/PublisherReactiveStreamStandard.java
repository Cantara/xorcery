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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.eclipse.jetty.io.ByteBufferOutputStream2;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.websocket.api.BatchMode;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.WebSocketListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.util.ArrayDeque;
import java.util.Base64;
import java.util.Deque;
import java.util.concurrent.Flow;
import java.util.function.Function;

/**
 * @author rickardoberg
 * @since 13/04/2022
 */
public class PublisherReactiveStreamStandard
        extends ServerReactiveStream
        implements WebSocketListener, Flow.Subscriber<Object> {
    private final static Logger logger = LogManager.getLogger(PublisherReactiveStreamStandard.class);

    private final String streamName;
    protected volatile Session session;

    private final Function<Configuration, Flow.Publisher<Object>> publisherFactory;
    private Configuration publisherConfiguration;
    private Flow.Publisher<Object> publisher;
    protected Flow.Subscription subscription;
    private boolean isComplete = false;

    private int requested;
    private final Deque<Object> queue = new ArrayDeque<>();
    private final ByteBufferOutputStream2 outputStream;

    protected final MessageWriter<Object> messageWriter;

    private final ObjectMapper objectMapper;
    protected final ByteBufferPool pool;
    protected final Marker marker;

    private boolean redundancyNotificationIssued = false;

    private long outstandingRequestAmount;

    public PublisherReactiveStreamStandard(String streamName,
                                           Function<Configuration, Flow.Publisher<Object>> publisherFactory,
                                           MessageWriter<Object> messageWriter,
                                           ObjectMapper objectMapper,
                                           ByteBufferPool pool) {
        this.streamName = streamName;
        this.publisherFactory = publisherFactory;
        this.messageWriter = messageWriter;
        this.objectMapper = objectMapper;
        this.pool = pool;
        marker = MarkerManager.getMarker(streamName);

        outputStream = new ByteBufferOutputStream2(pool, true);
    }

    // WebSocket
    @Override
    public void onWebSocketConnect(Session session) {
        if (logger.isTraceEnabled())
            logger.trace(marker, "onWebSocketConnect {}", session.getRemoteAddress().toString());

        this.session = session;
        session.getRemote().setBatchMode(BatchMode.ON);
    }

    @Override
    public void onWebSocketText(String message) {
        if (logger.isTraceEnabled())
            logger.trace(marker, "onWebSocketText {}", message);

        if (publisherConfiguration == null) {
            // Read JSON parameters
            try {
                ObjectNode configurationJson = (ObjectNode) objectMapper.readTree(message);
                publisherConfiguration = new Configuration.Builder(configurationJson)
                        .with(addUpgradeRequestConfiguration(session.getUpgradeRequest()))
                        .build();
                publisher = publisherFactory.apply(publisherConfiguration);
                publisher.subscribe(this);
            } catch (JsonProcessingException e) {
                session.close(StatusCode.BAD_PAYLOAD, e.getMessage());
            }
        } else {
            long requestAmount = Long.parseLong(message);

            if (subscription != null) {
                if (requestAmount == Long.MIN_VALUE) {
                    logger.info(marker, "Received cancel on websocket " + streamName);
                    session.close(StatusCode.NORMAL, "cancelled");
                } else {
                    if (logger.isDebugEnabled())
                        logger.debug(marker, "Received request:" + requestAmount);

                    Object item;
                    while (requestAmount > 0 && (item = queue.poll()) != null) {
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
    public void onWebSocketClose(int statusCode, String reason) {
        if (logger.isTraceEnabled())
            logger.trace(marker, "onWebSocketClose {} {}", statusCode, reason);

        if (subscription != null) {
            subscription.cancel();
            subscription = null;
        }
    }

    @Override
    public void onWebSocketError(Throwable cause) {
        if (logger.isTraceEnabled())
            logger.trace(marker, "onWebSocketError", cause);

        if (cause instanceof ClosedChannelException && subscription != null) {
            // Ignore
            subscription.cancel();
            subscription = null;
        } else {
            logger.error(marker, "Publisher websocket error", cause);
        }
    }

    // Subscriber
    @Override
    public void onSubscribe(Flow.Subscription subscription) {

        synchronized (session) {
            if (logger.isTraceEnabled())
                logger.trace(marker, "onSubscribe");

            this.subscription = subscription;

            if (outstandingRequestAmount > 0) {
                subscription.request(outstandingRequestAmount);
                outstandingRequestAmount = 0;
            }
        }
    }

    @Override
    public void onNext(Object item) {
        if (logger.isTraceEnabled())
            logger.trace(marker, "onNext {}", item.toString());

        synchronized (session) {
            if (session.isOpen() && requested > 0) {
                send(item);
                requested--;
            } else {
                queue.add(item);
            }
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

            session.getRemote().sendBytes(eventBuffer);

            if (queue.isEmpty()) {
                if (logger.isTraceEnabled())
                    logger.trace(marker, "flush");
                session.getRemote().flush();
            }
        } catch (Throwable t) {
            logger.error(marker, "Could not send event", t);
            session.close(StatusCode.SERVER_ERROR, t.getMessage());
        }
    }

    protected void writeItem(MessageWriter<Object> messageWriter, Object item, ByteBufferOutputStream2 outputStream) throws IOException {
        messageWriter.writeTo(item, outputStream);
    }

    public void onError(Throwable throwable) {
        if (logger.isTraceEnabled())
            logger.trace(marker, "onError", throwable);

        synchronized (session) {
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
    }

    public void onComplete() {
        if (logger.isTraceEnabled())
            logger.trace(marker, "onComplete");

        synchronized (session) {
            if (queue.isEmpty()) {
                session.close(StatusCode.NORMAL, "complete");
            } else {
                // Wait for requests to drain the remaining items
                isComplete = true;
            }
        }
    }
}
