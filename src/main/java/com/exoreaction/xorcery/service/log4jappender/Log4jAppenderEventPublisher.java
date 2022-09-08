package com.exoreaction.xorcery.service.log4jappender;

import com.exoreaction.xorcery.concurrent.NamedThreadFactory;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.disruptor.handlers.UnicastEventHandler;
import com.exoreaction.xorcery.jaxrs.AbstractFeature;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import com.exoreaction.xorcery.service.conductor.api.Conductor;
import com.exoreaction.xorcery.service.log4jappender.log4j.DisruptorAppender;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreams;
import com.exoreaction.xorcery.service.reactivestreams.api.WithMetadata;
import com.exoreaction.xorcery.service.reactivestreams.helper.ClientPublisherConductorListener;
import com.lmax.disruptor.BlockingWaitStrategy;
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

import java.util.concurrent.Flow;

@Singleton
@WebListener
public class Log4jAppenderEventPublisher
        implements ContainerLifecycleListener, Flow.Publisher<WithMetadata<LogEvent>> {

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
    private ServiceResourceObject sro;
    private Configuration configuration;

    private final Disruptor<WithMetadata<LogEvent>> disruptor;
    private final UnicastEventHandler<WithMetadata<LogEvent>> unicastEventHandler = new UnicastEventHandler<>();

    @Inject
    public Log4jAppenderEventPublisher(ReactiveStreams reactiveStreams, Conductor conductor,
                                       @Named(SERVICE_TYPE) ServiceResourceObject sro,
                                       Configuration configuration) {
        this.reactiveStreams = reactiveStreams;
        this.sro = sro;
        this.configuration = configuration;

        disruptor =
                new Disruptor<>(WithMetadata::new, 4096, new NamedThreadFactory("Log4jDisruptor-"),
                        ProducerType.MULTI,
                        new BlockingWaitStrategy());

        disruptor.handleEventsWith(new LoggingMetadataEventHandler(configuration))
                .then(unicastEventHandler);

        conductor.addConductorListener(new ClientPublisherConductorListener(sro.serviceIdentifier(), cfg -> this, getClass(), "logevents", reactiveStreams));
    }

    @Override
    public void onStartup(Container container) {
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
    public void subscribe(Flow.Subscriber<? super WithMetadata<LogEvent>> subscriber) {
        LoggerContext lc = (LoggerContext) LogManager.getContext(false);
        DisruptorAppender appender = lc.getConfiguration().getAppender("DISRUPTOR");

        subscriber.onSubscribe(unicastEventHandler.add(subscriber, new Flow.Subscription() {
            @Override
            public void request(long n) {
                // Ignore for now
            }

            @Override
            public void cancel() {
                unicastEventHandler.remove(subscriber);
            }
        }));
    }
}
