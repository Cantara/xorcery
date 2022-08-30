package com.exoreaction.xorcery.service.reactivestreams;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.jsonapi.model.Link;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveEventStreams;
import com.exoreaction.xorcery.service.reactivestreams.api.ServiceIdentifier;
import com.exoreaction.xorcery.service.reactivestreams.resources.websocket.PublishWebSocketEndpoint;
import com.exoreaction.xorcery.service.reactivestreams.resources.websocket.SubscribeWebSocketEndpoint;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;

public record PublishingProcess<T>(WebSocketClient webSocketClient, ObjectMapper objectMapper, Timer timer,
                                   Logger logger,
                                   ByteBufferPool byteBufferPool, MessageBodyReader<Object> reader,
                                   MessageBodyWriter<T> writer, ServiceIdentifier selfServiceIdentifier,
                                   Link websocketLink,
                                   Configuration publisherConfiguration,
                                   Configuration subscriberConfiguration,
                                   ReactiveEventStreams.Publisher<T> publisher,
                                   Type eventType, CompletableFuture<Void> result
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
                Marker marker = MarkerManager.getMarker(selfServiceIdentifier.toString());

                URI websocketEndpointUri = websocketLink.getHrefAsUri();
                webSocketClient.connect(new PublishWebSocketEndpoint<T>(websocketEndpointUri.toASCIIString(), publisher, writer, reader, eventType, publisherConfiguration, subscriberConfiguration,
                                objectMapper, byteBufferPool, marker, this), websocketEndpointUri)
                        .whenComplete(this::complete);
            } catch (IOException e) {
                logger.error("Could not subscribe to " + websocketLink.getHref(), e);

                retry();
            }
        });
    }

    private void complete(Session session, Throwable throwable) {
        if (throwable != null) {
            logger.error("Could not subscribe to " + websocketLink.getHref(), throwable);
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
