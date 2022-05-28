package com.exoreaction.reactiveservices.service.log4jappender;

import com.exoreaction.reactiveservices.disruptor.Event;
import com.exoreaction.reactiveservices.jaxrs.AbstractFeature;
import com.exoreaction.reactiveservices.server.Server;
import com.exoreaction.reactiveservices.service.log4jappender.log4j.DisruptorAppender;
import com.exoreaction.reactiveservices.server.model.ServiceResourceObject;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ReactiveStreams;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ReactiveEventStreams;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ReactiveEventStreams.Publisher;
import com.lmax.disruptor.EventSink;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.servlet.annotation.WebListener;
import jakarta.ws.rs.ext.Provider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Singleton
@WebListener
public class Log4jAppenderEventPublisher
        implements ContainerLifecycleListener, Publisher<LogEvent> {

    public static final String SERVICE_TYPE = "log4jappender";

    @Provider
    public static class Feature
            extends AbstractFeature {
        @Override
        protected String serviceType() {
            return SERVICE_TYPE;
        }

        @Override
        protected void buildResourceObject(ServiceResourceObject.Builder builder) {
            builder.websocket("logevents", "ws/logevents");
        }

        @Override
        protected void configure() {
            context.register(Log4jAppenderEventPublisher.class);
        }
    }

    private final ReactiveStreams reactiveStreams;
    private Server server;
    private ServiceResourceObject sro;

    @Inject
    public Log4jAppenderEventPublisher(ReactiveStreams reactiveStreams, Server server, @Named(SERVICE_TYPE) ServiceResourceObject sro) {
        this.reactiveStreams = reactiveStreams;
        this.server = server;
        this.sro = sro;
    }

    @Override
    public void onStartup(Container container) {
        sro.getLinkByRel("logevents").ifPresent(link ->
        {
            reactiveStreams.publish(sro.serviceIdentifier(), link, this);
        });
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

        final AtomicReference<EventSink<Event<LogEvent>>> handler = new AtomicReference<>();
        handler.set(subscriber.onSubscribe(new ReactiveEventStreams.Subscription() {
            @Override
            public void request(long n) {
                // Ignore for now
            }

            @Override
            public void cancel() {
                appender.getSubscribers().remove(handler.get());
            }
        }));
        appender.getSubscribers().add(handler.get());
    }
}
