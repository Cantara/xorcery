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
package com.exoreaction.xorcery.log4jsubscriber;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.reactivestreams.api.WithMetadata;
import com.exoreaction.xorcery.reactivestreams.api.server.ReactiveStreamsServer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.inject.Inject;
import org.apache.logging.log4j.*;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.Optional;

@Service(name = "log4jsubscriber")
@RunLevel(6)
public class Log4jSubscriberService {

    private final Log4jSubscriberConfiguration log4jSubscriberConfiguration;

    @Inject
    public Log4jSubscriberService(ReactiveStreamsServer reactiveStreams,
                                  Configuration configuration) {
        log4jSubscriberConfiguration = new Log4jSubscriberConfiguration(configuration.getConfiguration("log4jsubscriber"));

        reactiveStreams.subscriber(log4jSubscriberConfiguration.getSubscriberStream(), LogSubscriber::new, LogSubscriber.class);
    }

    static class LogSubscriber
            implements Subscriber<WithMetadata<JsonNode>> {

        private final Logger logger;
        private Subscription subscription;

        public LogSubscriber(Configuration configuration) {

            logger = LogManager.getLogger(configuration.getString("category").orElse(Log4jSubscriberService.class.getName()));
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            this.subscription = subscription;
            subscription.request(512);
        }

        @Override
        public void onNext(WithMetadata<JsonNode> item) {

            if (item.event().has("loggerName")) {
                ObjectNode event = (ObjectNode) item.event();
                Logger eventLogger = LogManager.getLogger(event.get("loggerName").textValue());
                Level level = Level.getLevel(event.get("level").textValue());
                Marker marker = Optional.ofNullable(event.path("marker").textValue()).map(MarkerManager::getMarker).orElse(null);
                String message = event.path("message").textValue();
                String thrown = null;
                if (event.has("thrown")) {
                    JsonNode thrownNode = event.path("thrown");
                    thrown = thrownNode.path("name").textValue() + ": " + thrownNode.path("message").textValue();
                    thrown += "\n" + thrownNode.path("extendedStackTrace").textValue();
                }
                if (thrown != null) {
                    message += "\n" + thrown;
                }
                ThreadContext.put("log4jsubscriber", Boolean.TRUE.toString());
                String instanceId = event.path("instanceId").textValue();
                if (instanceId != null) {
                    ThreadContext.put("instanceId", instanceId);
                }
                eventLogger.log(level, marker, message);
                ThreadContext.clearMap();
            } else {
                logger.info(item.event().toString());
            }

            subscription.request(1);
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
