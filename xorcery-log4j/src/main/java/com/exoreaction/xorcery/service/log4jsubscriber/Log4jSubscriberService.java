package com.exoreaction.xorcery.service.log4jsubscriber;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreamsServer;
import com.exoreaction.xorcery.service.reactivestreams.api.WithMetadata;
import com.exoreaction.xorcery.service.reactivestreams.providers.WithMetadataMessageReaderFactory;
import com.exoreaction.xorcery.service.reactivestreams.spi.MessageReader;
import com.exoreaction.xorcery.service.reactivestreams.spi.MessageWorkers;
import com.exoreaction.xorcery.util.ByteBufferBackedInputStream;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.runlevel.RunLevel;
import org.glassfish.hk2.utilities.reflection.ParameterizedTypeImpl;
import org.jvnet.hk2.annotations.Service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Flow;

@Service(name = "log4jsubscriber")
@RunLevel(6)
public class Log4jSubscriberService {

    private final Log4jSubscriberConfiguration log4jSubscriberConfiguration;
    private final Provider<MessageWorkers> messageWorkers;

    @Inject
    public Log4jSubscriberService(ReactiveStreamsServer reactiveStreams,
                                  Configuration configuration,
                                  Provider<MessageWorkers> messageWorkers) {
        this.messageWorkers = messageWorkers;

        log4jSubscriberConfiguration = new Log4jSubscriberConfiguration(configuration.getConfiguration("log4jsubscriber"));

        reactiveStreams.subscriber(log4jSubscriberConfiguration.getSubscriberStream(), LogSubscriber::new, LogSubscriber.class);
    }

    public class LogSubscriber
            implements Flow.Subscriber<ByteBuffer> {

        private final Logger logger;
        private MessageReader<Object> messageReader;

        public LogSubscriber(Configuration configuration) {

            logger = LogManager.getLogger(configuration.getString("category").orElse(Log4jSubscriberService.class.getName()));

            String type = configuration.getString("type").orElse("java.lang.String");
            if (type.startsWith(WithMetadata.class.getName())) {
                messageReader = new WithMetadataMessageReaderFactory(messageWorkers::get).newReader(WithMetadata.class, new ParameterizedTypeImpl(WithMetadata.class, String.class), "text/plain");
            } else {
                messageReader = messageWorkers.get().newReader(String.class, String.class, "text/plain");
            }
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            subscription.request(512);
        }

        @Override
        public void onNext(ByteBuffer item) {

            try {
                Object message = new String(new ByteBufferBackedInputStream(item).readAllBytes(), StandardCharsets.UTF_8);
                logger.info(message.toString());
            } catch (IOException e) {
                logger.error("Could not deserialize message", e);
            }
/*
            try {
                Object message = messageReader.readFrom(new ByteBufferBackedInputStream(item));
                if (message instanceof WithMetadata<?> withMetadata) {
                    logger.info(withMetadata.metadata().json() + ":" + withMetadata.event().toString());
                } else {
                    logger.info(message.toString());
                }
            } catch (IOException e) {
                logger.error("Could not deserialize message", e);
            }
*/
        }

        @Override
        public void onError(Throwable throwable) {
            logger.error("Stream error", throwable);
        }

        @Override
        public void onComplete() {
            logger.info("Complete");
        }
    }
}
