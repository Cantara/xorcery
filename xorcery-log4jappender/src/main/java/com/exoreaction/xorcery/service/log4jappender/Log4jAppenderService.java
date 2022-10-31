package com.exoreaction.xorcery.service.log4jappender;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.core.TopicSubscribers;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import com.exoreaction.xorcery.service.conductor.helpers.ClientPublisherGroupListener;
import com.exoreaction.xorcery.service.log4jappender.log4j.DisruptorAppender;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreams;
import com.exoreaction.xorcery.service.reactivestreams.api.WithMetadata;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.messaging.Topic;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.jvnet.hk2.annotations.Service;

import java.util.concurrent.Flow;

@Service
@Named("log4jappender")
public class Log4jAppenderService {

    public static final String SERVICE_TYPE = "log4jappender";
    private final DisruptorAppender appender;

    @Inject
    public Log4jAppenderService(Topic<ServiceResourceObject> registryTopic,
                                ReactiveStreams reactiveStreams,
                                ServiceLocator serviceLocator,
                                Configuration configuration) {
        ServiceResourceObject sro = new ServiceResourceObject.Builder(() -> configuration, SERVICE_TYPE)
                .websocket("logevents", "ws/logevents")
                .build();

        LoggerContext lc = (LoggerContext) LogManager.getContext(false);
        appender = lc.getConfiguration().getAppender("DISRUPTOR");
        appender.setConfiguration(configuration);

        sro.getLinkByRel("logevents").ifPresent(link ->
        {
            reactiveStreams.publisher(link.getHrefAsUri().getPath(), cfg -> new LogPublisher(), LogPublisher.class);
        });

        TopicSubscribers.addSubscriber(serviceLocator,new ClientPublisherGroupListener(sro.getServiceIdentifier(), cfg -> new LogPublisher(), LogPublisher.class, null, reactiveStreams));
        registryTopic.publish(sro);
    }

    public class LogPublisher
            implements Flow.Publisher<WithMetadata<LogEvent>> {
        @Override
        public void subscribe(Flow.Subscriber<? super WithMetadata<LogEvent>> subscriber) {
            subscriber.onSubscribe(appender.getUnicastEventHandler().add(subscriber));
        }
    }
}
