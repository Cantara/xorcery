package com.exoreaction.xorcery.service.log4jpublisher;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.service.log4jpublisher.log4j.Log4jPublisherAppender;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreamsClient;
import com.exoreaction.xorcery.service.reactivestreams.api.WithMetadata;
import jakarta.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import java.util.concurrent.Flow;

@Service(name="log4jpublisher")
@RunLevel(8)
public class Log4jPublisherService {

    private final Log4jPublisherAppender appender;

    @Inject
    public Log4jPublisherService(ReactiveStreamsClient reactiveStreams,
                                 Configuration configuration) {

        LoggerContext lc = (LoggerContext) LogManager.getContext(false);
        appender = lc.getConfiguration().getAppender("Log4jPublisher");
        appender.setConfiguration(configuration);

        reactiveStreams.publish(configuration.getString("log4jpublisher.subscriber.authority").orElseThrow(), configuration.getString("log4jpublisher.subscriber.stream").orElseThrow(),
                () -> configuration.getConfiguration("log4jpublisher.subscriber.configuration"), new LogPublisher(), LogPublisher.class, configuration.getConfiguration("log4jpublisher.publisher.configuration"));
    }

    public class LogPublisher
            implements Flow.Publisher<WithMetadata<LogEvent>> {
        @Override
        public void subscribe(Flow.Subscriber<? super WithMetadata<LogEvent>> subscriber) {
            subscriber.onSubscribe(appender.getUnicastEventHandler().add(subscriber));
        }
    }
}
