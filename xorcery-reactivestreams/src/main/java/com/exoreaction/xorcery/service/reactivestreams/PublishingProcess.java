package com.exoreaction.xorcery.service.reactivestreams;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.service.reactivestreams.resources.websocket.PublishWebSocketEndpoint;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.ForkJoinPool;

public record PublishingProcess(WebSocketClient webSocketClient, ObjectMapper objectMapper, Timer timer,
                                Logger logger,
                                ByteBufferPool byteBufferPool,
                                MessageBodyReader<Object> resultReader,
                                MessageBodyWriter<Object> eventWriter,
                                Type eventType,
                                Type resultType,
                                URI subscriberWebsocketUri,
                                Configuration subscriberConfiguration,
                                Flow.Publisher<Object> publisher, CompletableFuture<Void> result
) {
    public void start() {
        if (result.isDone()) {
            return;
        }

        if (!webSocketClient.isStarted()) {
            retry();
        }

        ForkJoinPool.commonPool().execute(() ->
        {
            try {
                webSocketClient.connect(new PublishWebSocketEndpoint(
                        subscriberWebsocketUri.toASCIIString(),
                                publisher,
                                eventWriter,
                                resultReader,
                                eventType,
                                resultType,
                                subscriberConfiguration,
                                objectMapper,
                                byteBufferPool,
                                this), subscriberWebsocketUri)
                        .whenComplete(this::complete);
            } catch (IOException e) {
                logger.error("Could not subscribe to " + subscriberWebsocketUri.toASCIIString(), e);

                retry();
            }
        });
    }

    private void complete(Session session, Throwable throwable) {
        if (throwable != null) {
            logger.error("Could not subscribe to " + subscriberWebsocketUri.toASCIIString(), throwable);
            retry();
        }
    }

    public void retry() {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                start();
            }
        }, 10000);
    }
}
