package com.exoreaction.xorcery.service.reactivestreams;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.jsonapi.model.Link;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveEventStreams;
import com.exoreaction.xorcery.service.reactivestreams.api.ServiceIdentifier;
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

public record SubscriptionProcess<T>(WebSocketClient webSocketClient, ObjectMapper objectMapper, Timer timer,
                                     Logger logger,
                                     ByteBufferPool byteBufferPool, MessageBodyReader<Object> reader,
                                     MessageBodyWriter<Object> writer, ServiceIdentifier selfServiceIdentifier,
                                     Link websocketLink,
                                     Configuration publisherConfiguration,
                                     Configuration subscriberConfiguration,
                                     ReactiveEventStreams.Subscriber<T> subscriber,
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
                webSocketClient.connect(new SubscribeWebSocketEndpoint<T>(subscriber(), reader, writer, objectMapper, eventType, marker,
                                byteBufferPool, this, websocketEndpointUri.toASCIIString(), publisherConfiguration, subscriberConfiguration), websocketEndpointUri)
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
