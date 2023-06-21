/*
 * Copyright © 2022 eXOReaction AS (rickard@exoreaction.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.exoreaction.xorcery.log4jpublisher;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.log4jpublisher.log4j.Log4jPublisherAppender;
import com.exoreaction.xorcery.reactivestreams.api.client.ClientConfiguration;
import com.exoreaction.xorcery.reactivestreams.api.client.ReactiveStreamsClient;
import com.exoreaction.xorcery.reactivestreams.api.WithMetadata;
import jakarta.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import java.util.concurrent.CompletionException;
import java.util.concurrent.Flow;

@Service(name = "log4jpublisher")
@RunLevel(8)
public class Log4jPublisherService {

    private final Log4jPublisherAppender appender;

    @Inject
    public Log4jPublisherService(ReactiveStreamsClient reactiveStreams,
                                 Configuration configuration, Logger logger) {

        Log4jPublisherConfiguration log4jPublisherConfiguration = new Log4jPublisherConfiguration(configuration.getConfiguration("log4jpublisher"));

        LoggerContext lc = (LoggerContext) LogManager.getContext(false);
        String appenderName = log4jPublisherConfiguration.getAppenderName();
        appender = lc.getConfiguration().getAppender(appenderName);
        if (appender != null) {
            appender.setConfiguration(configuration);

            reactiveStreams.publish(log4jPublisherConfiguration.getSubscriberURI().orElse(null), log4jPublisherConfiguration.getSubscriberStream(),
                            log4jPublisherConfiguration::getSubscriberConfiguration, new LogPublisher(), LogPublisher.class, new ClientConfiguration(log4jPublisherConfiguration.getPublisherConfiguration()))
                    .exceptionally(throwable ->
                    {
                        if (throwable instanceof CompletionException)
                            logger.error("Could not publish log messages", throwable);
                        return null;
                    });
        } else {
            LogManager.getLogger(getClass()).warn("No appender {} added to Log4j2 configuration", appenderName);
        }
    }

    public class LogPublisher
            implements Flow.Publisher<WithMetadata<LogEvent>> {
        @Override
        public void subscribe(Flow.Subscriber<? super WithMetadata<LogEvent>> subscriber) {
            subscriber.onSubscribe(appender.getUnicastEventHandler().add(subscriber));
        }
    }
}
