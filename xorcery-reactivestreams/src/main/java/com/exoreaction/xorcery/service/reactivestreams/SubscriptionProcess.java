package com.exoreaction.xorcery.service.reactivestreams;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.rest.RestProcess;
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
import java.util.concurrent.Flow;
import java.util.concurrent.ForkJoinPool;

public record SubscriptionProcess(WebSocketClient webSocketClient,
                                  ObjectMapper objectMapper,
                                  Timer timer,
                                  Logger logger,
                                  ByteBufferPool byteBufferPool,
                                  MessageBodyReader<Object> eventReader,
                                  MessageBodyWriter<Object> resultWriter,
                                  Type eventType,
                                  Type resultType,
                                  URI publisherWebsocketUri,
                                  Configuration publisherConfiguration,
                                  Flow.Subscriber<Object> subscriber,
                                  CompletableFuture<Void> result
)
        implements RestProcess<Void> {
    public void start() {
        if (result.isDone()) {
            return;
        }

        if (!webSocketClient.isStarted()) {
            retry();
        }

        ForkJoinPool.commonPool().execute(() ->
        {
            Marker marker = MarkerManager.getMarker(publisherWebsocketUri.toASCIIString());

            try {
                webSocketClient.connect(new SubscribeWebSocketEndpoint(
                                subscriber(),
                                eventReader,
                                resultWriter,
                                eventType,
                                resultType,
                                byteBufferPool,
                                this,
                                publisherWebsocketUri.toASCIIString(),
                                publisherConfiguration), publisherWebsocketUri)
                        .whenComplete(this::complete);
            } catch (IOException e) {
                logger.error(marker, "Could not subscribe to " + publisherWebsocketUri.toASCIIString(), e);

                retry();
            }
        });
    }

    private void complete(Session session, Throwable throwable) {
        if (throwable != null) {
            logger.error("Could not subscribe to " + publisherWebsocketUri, throwable);
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
