package com.exoreaction.xorcery.service.log4jappender;

import com.exoreaction.xorcery.concurrent.NamedThreadFactory;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.disruptor.Event;
import com.exoreaction.xorcery.disruptor.handlers.BroadcastEventHandler;
import com.exoreaction.xorcery.jaxrs.AbstractFeature;
import com.exoreaction.xorcery.server.Server;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import com.exoreaction.xorcery.service.log4jappender.log4j.DisruptorAppender;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveEventStreams;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveEventStreams.Publisher;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreams;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventSink;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
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
    private Configuration configuration;

    private final Disruptor<Event<LogEvent>> disruptor;
    private final List<EventSink<Event<LogEvent>>> subscribers = new CopyOnWriteArrayList<>();

    @Inject
    public Log4jAppenderEventPublisher(ReactiveStreams reactiveStreams, Server server,
                                       @Named(SERVICE_TYPE) ServiceResourceObject sro,
                                       Configuration configuration) {
        this.reactiveStreams = reactiveStreams;
        this.server = server;
        this.sro = sro;
        this.configuration = configuration;

        disruptor =
                new Disruptor<>(Event::new, 4096, new NamedThreadFactory("Log4jDisruptor-"),
                        ProducerType.MULTI,
                        new BlockingWaitStrategy());

        disruptor.handleEventsWith(
                        new LoggingMetadataEventHandler(configuration))
                .then(new BroadcastEventHandler<>(subscribers));
    }

    @Override
    public void onStartup(Container container) {
        sro.getLinkByRel("logevents").ifPresent(link ->
        {
            reactiveStreams.publish(sro.serviceIdentifier(), link, this);
        });

        disruptor.start();
        LoggerContext lc = (LoggerContext) LogManager.getContext(false);
        DisruptorAppender appender = lc.getConfiguration().getAppender("DISRUPTOR");
        appender.setEventSink(disruptor.getRingBuffer());
    }

    @Override
    public void onReload(Container container) {
    }

    @Override
    public void onShutdown(Container container) {
        disruptor.shutdown();
    }

    @Override
    public void subscribe(ReactiveEventStreams.Subscriber<LogEvent> subscriber, Configuration configuration) {
        LoggerContext lc = (LoggerContext) LogManager.getContext(false);
        DisruptorAppender appender = lc.getConfiguration().getAppender("DISRUPTOR");

        final AtomicReference<EventSink<Event<LogEvent>>> handler = new AtomicReference<>();
        handler.set(subscriber.onSubscribe(new ReactiveEventStreams.Subscription() {
            @Override
            public void request(long n) {
                // Ignore for now
            }

            @Override
            public void cancel() {
                subscribers.remove(handler.get());
            }
        }));
        subscribers.add(handler.get());
    }
}
