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
package com.exoreaction.xorcery.requestlogpublisher;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.configuration.InstanceConfiguration;
import com.exoreaction.xorcery.log4jpublisher.LoggingMetadata;
import com.exoreaction.xorcery.metadata.Metadata;
import com.exoreaction.xorcery.reactivestreams.api.WithMetadata;
import com.exoreaction.xorcery.reactivestreams.api.client.ClientConfiguration;
import com.exoreaction.xorcery.reactivestreams.api.client.ReactiveStreamsClient;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.inject.Inject;
import org.eclipse.jetty.server.Server;
import org.glassfish.hk2.api.PreDestroy;
import org.jvnet.hk2.annotations.Service;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

@Service(name = "requestlogpublisher")
public class RequestLogPublisherService
        implements PreDestroy {

    @Inject
    public RequestLogPublisherService(ReactiveStreamsClient reactiveStreams,
                                      Server server,
                                      Configuration configuration) {

        RequestLogConfiguration requestLogConfiguration = new RequestLogConfiguration(configuration.getConfiguration("requestlog"));
        RequestLogPublisher requestLogPublisher = new RequestLogPublisher();
        reactiveStreams.publish(requestLogConfiguration.getSubscriberURI().orElse(null), requestLogConfiguration.getSubscriberStream(),
                requestLogConfiguration::getSubscriberConfiguration, requestLogPublisher, RequestLogPublisher.class, new ClientConfiguration(requestLogConfiguration.getPublisherConfiguration()));

        JsonRequestLog requestLog = new JsonRequestLog(new LoggingMetadata.Builder(new Metadata.Builder())
                .configuration(configuration)
                .build(), requestLogPublisher);
        server.setRequestLog(requestLog);
    }

    @Override
    public void preDestroy() {

    }

    public static class RequestLogPublisher
            implements Publisher<WithMetadata<ObjectNode>> {

        private Subscriber<? super WithMetadata<ObjectNode>> subscriber;

        @Override
        public void subscribe(Subscriber<? super WithMetadata<ObjectNode>> subscriber) {
            this.subscriber = subscriber;
        }

        public void send(WithMetadata<ObjectNode> event) {
            subscriber.onNext(event);
        }
    }
}
