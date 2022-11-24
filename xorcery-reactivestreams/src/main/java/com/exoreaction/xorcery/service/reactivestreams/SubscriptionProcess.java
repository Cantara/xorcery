package com.exoreaction.xorcery.service.reactivestreams;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.rest.RestProcess;
import com.exoreaction.xorcery.service.dns.client.api.DnsLookup;
import com.exoreaction.xorcery.service.reactivestreams.resources.websocket.SubscribeWebSocketEndpoint;
import com.exoreaction.xorcery.service.reactivestreams.spi.MessageReader;
import com.exoreaction.xorcery.service.reactivestreams.spi.MessageWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.ForkJoinPool;

public record SubscriptionProcess(WebSocketClient webSocketClient,
                                  DnsLookup dnsLookup,
                                  ObjectMapper objectMapper,
                                  Timer timer,
                                  Logger logger,
                                  ByteBufferPool byteBufferPool,
                                  MessageReader<Object> eventReader,
                                  MessageWriter<Object> resultWriter,
                                  URI publisherWebsocketUri,
                                  List<URI> possiblePublisherWebsocketUris,
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
            if (possiblePublisherWebsocketUris.isEmpty()) {
                dnsLookup.resolve(publisherWebsocketUri)
                        .thenApply(uris ->
                        {
                            possiblePublisherWebsocketUris.addAll(uris);
                            return possiblePublisherWebsocketUris.remove(0);
                        }).thenCompose(this::connect)
                        .whenComplete(this::complete);
            } else {
                connect(possiblePublisherWebsocketUris.remove(0))
                        .whenComplete(this::complete);
            }
        });
    }

    private CompletableFuture<Session> connect(URI selectedPublisherUri) {
        try {
            return webSocketClient.connect(new SubscribeWebSocketEndpoint(
                    subscriber(),
                    eventReader,
                    resultWriter,
                    byteBufferPool,
                    this,
                    selectedPublisherUri.toASCIIString(),
                    publisherConfiguration), selectedPublisherUri);
        } catch (IOException e) {
            Marker marker = MarkerManager.getMarker(publisherWebsocketUri.toASCIIString());
            logger.error(marker, "Could not subscribe to " + selectedPublisherUri.toASCIIString(), e);
            return CompletableFuture.failedFuture(e);
        }
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
