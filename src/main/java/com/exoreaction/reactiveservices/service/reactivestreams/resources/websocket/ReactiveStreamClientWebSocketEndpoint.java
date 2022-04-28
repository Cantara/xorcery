package com.exoreaction.reactiveservices.service.reactivestreams.resources.websocket;

import com.exoreaction.reactiveservices.concurrent.NamedThreadFactory;
import com.exoreaction.reactiveservices.disruptor.Event;
import com.exoreaction.reactiveservices.disruptor.handlers.MetadataDeserializerEventHandler;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ReactiveEventStreams;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ServiceLinkReference;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.MarkerManager;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;

public class ReactiveStreamClientWebSocketEndpoint<T>
        implements WebSocketListener {

    private static final Logger logger = LogManager.getLogger(ReactiveStreamClientWebSocketEndpoint.class);

    private final ServiceLinkReference link;
    private final ReactiveEventStreams.Subscriber<T> subscriber;
    private Disruptor<Event<T>> disruptor;
    private ByteBuffer headers;

    public ReactiveStreamClientWebSocketEndpoint(ServiceLinkReference link, ReactiveEventStreams.Subscriber<T> subscriber) {
        this.link = link;
        this.subscriber = subscriber;
    }

    @Override
    public void onWebSocketText(String message) {
        System.out.println("Text:" + message);
    }

    @Override
    public void onWebSocketBinary(byte[] payload, int offset, int len) {
        if (headers == null) {
            headers = ByteBuffer.wrap(payload, offset, len);
        } else {
            ByteBuffer body = ByteBuffer.wrap(payload, offset, len);
            disruptor.publishEvent((holder, seq, h, b) ->
            {
                holder.headers = h;
                holder.body = b;
            }, headers, body);
            headers = null;
        }
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        System.out.printf("Close:%d %s%n", statusCode, reason);
        disruptor.shutdown();
    }

    @Override
    public void onWebSocketConnect(Session session) {
        disruptor =
                new Disruptor<>(Event::new, 4096, new NamedThreadFactory("ReactiveStream-" + link.service().id() + "-" + link.service().type() + "-" + link.rel() + "-"),
                        ProducerType.SINGLE,
                        new BlockingWaitStrategy());

        EventHandler<Event<T>> handler = subscriber.onSubscribe(new ReactiveEventStreams.Subscription() {
            @Override
            public void request(long n) {
                try {
                    session.getRemote().sendString(Long.toString(n));
                } catch (IOException e) {
                    logger.error(MarkerManager.getMarker(link.toString()), "Could not send request", e);
                }
            }

            @Override
            public void cancel() {
                session.close();
            }
        });

        disruptor.handleEventsWith(new MetadataDeserializerEventHandler())
                .then(handler);
        disruptor.start();

        logger.info(MarkerManager.getMarker(link.toString()), "Connected to {}", session.getUpgradeRequest().getRequestURI());
    }

    @Override
    public void onWebSocketError(Throwable cause) {
        StringWriter sw = new StringWriter();
        cause.printStackTrace(new PrintWriter(sw));

        System.out.printf("Error:%s%n", sw.toString());
    }
}
