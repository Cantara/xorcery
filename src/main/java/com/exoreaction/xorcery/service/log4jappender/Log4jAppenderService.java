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
import com.exoreaction.xorcery.service.requestlog.RequestLogService;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
public class Log4jAppenderService
        implements ContainerLifecycleListener {

    public static final String SERVICE_TYPE = "log4jappender";
    private final DisruptorAppender appender;

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
            context.register(Log4jAppenderService.class);
        }
    }

    private final ReactiveStreams reactiveStreams;
    private ServiceResourceObject sro;


    @Inject
    public Log4jAppenderService(ReactiveStreams reactiveStreams, Conductor conductor,
                                @Named(SERVICE_TYPE) ServiceResourceObject sro,
                                Configuration configuration) {
        this.reactiveStreams = reactiveStreams;
        this.sro = sro;

        LoggerContext lc = (LoggerContext) LogManager.getContext(false);
        appender = lc.getConfiguration().getAppender("DISRUPTOR");
        appender.setConfiguration(configuration);

        conductor.addConductorListener(new ClientPublisherConductorListener(sro.serviceIdentifier(), cfg -> new LogPublisher(), LogPublisher.class, null, reactiveStreams));
    }

    @Override
    public void onStartup(Container container) {
        sro.getLinkByRel("logevents").ifPresent(link ->
        {
            reactiveStreams.publisher(link.getHrefAsUri().getPath(), cfg -> new LogPublisher(), LogPublisher.class);
        });
    }

    @Override
    public void onReload(Container container) {
    }

    @Override
    public void onShutdown(Container container) {
    }

    public class LogPublisher
            implements Flow.Publisher<WithMetadata<LogEvent>> {
        @Override
        public void subscribe(Flow.Subscriber<? super WithMetadata<LogEvent>> subscriber) {
            subscriber.onSubscribe(appender.getUnicastEventHandler().add(subscriber, new Flow.Subscription() {
                @Override
                public void request(long n) {
                    // Ignore for now
                }

                @Override
                public void cancel() {
                }
            }));
        }
    }
}
