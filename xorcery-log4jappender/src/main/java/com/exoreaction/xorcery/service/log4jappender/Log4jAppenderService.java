package com.exoreaction.xorcery.service.log4jappender;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.server.api.ServiceResourceObjects;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import com.exoreaction.xorcery.service.log4jappender.log4j.DisruptorAppender;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreamsClient;
import com.exoreaction.xorcery.service.reactivestreams.api.WithMetadata;
import com.exoreaction.xorcery.service.requestlog.RequestLogService;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.annotations.Service;

import java.util.concurrent.Flow;

@Service(name="log4jappender")
public class Log4jAppenderService {

    private final DisruptorAppender appender;

    @Inject
    public Log4jAppenderService(ReactiveStreamsClient reactiveStreams,
                                Configuration configuration) {

        LoggerContext lc = (LoggerContext) LogManager.getContext(false);
        appender = lc.getConfiguration().getAppender("DISRUPTOR");
        appender.setConfiguration(configuration);

        reactiveStreams.publish(configuration.getString("requestlog.host").orElseThrow(), configuration.getString("requestlog.stream").orElseThrow(),
                () -> configuration.getConfiguration("requestlog.configuration"), new LogPublisher(), LogPublisher.class, Configuration.empty());
    }

    public class LogPublisher
            implements Flow.Publisher<WithMetadata<LogEvent>> {
        @Override
        public void subscribe(Flow.Subscriber<? super WithMetadata<LogEvent>> subscriber) {
            subscriber.onSubscribe(appender.getUnicastEventHandler().add(subscriber));
        }
    }
}
