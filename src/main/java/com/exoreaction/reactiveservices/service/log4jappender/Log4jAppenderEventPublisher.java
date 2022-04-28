package com.exoreaction.reactiveservices.service.log4jappender;

import com.exoreaction.reactiveservices.disruptor.Event;
import com.exoreaction.reactiveservices.server.Server;
import com.exoreaction.reactiveservices.service.log4jappender.log4j.DisruptorAppender;
import com.exoreaction.reactiveservices.service.log4jappender.log4j.Log4jSerializeEventHandler;
import com.exoreaction.reactiveservices.service.reactivestreams.ReactiveStreams;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ReactiveEventStreams;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ReactiveEventStreams.Publisher;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ServiceReference;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ServiceLinkReference;
import com.lmax.disruptor.EventHandler;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.layout.JsonLayout;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Singleton
public class Log4jAppenderEventPublisher
        implements ContainerLifecycleListener, Publisher<LogEvent> {
    private final ReactiveStreams reactiveStreams;
    private Server server;
    private ServiceLinkReference streamReference;

    @Inject
    public Log4jAppenderEventPublisher(ReactiveStreams reactiveStreams, Server server) {
        this.reactiveStreams = reactiveStreams;
        this.server = server;
        streamReference = new ServiceLinkReference(new ServiceReference("log4jappender", server.getServerId()), "logevents");
    }

    @Override
    public void onStartup(Container container) {

        reactiveStreams.publish(streamReference, this, new Log4jSerializeEventHandler(JsonLayout.newBuilder().build()));
    }

    @Override
    public void onReload(Container container) {

    }

    @Override
    public void onShutdown(Container container) {

    }

    @Override
    public void subscribe(ReactiveEventStreams.Subscriber<LogEvent> subscriber, Map<String, String> parameters) {
        LoggerContext lc = (LoggerContext) LogManager.getContext(false);
        DisruptorAppender appender = lc.getConfiguration().getAppender("Disruptor");

        final AtomicReference<EventHandler<Event<LogEvent>>> handler = new AtomicReference<>();
        handler.set(subscriber.onSubscribe(new ReactiveEventStreams.Subscription() {
            @Override
            public void request(long n) {
                // Ignore for now
            }

            @Override
            public void cancel() {
                appender.getConsumers().remove(handler.get());
            }
        }));
        appender.getConsumers().add(handler.get());
    }
}
