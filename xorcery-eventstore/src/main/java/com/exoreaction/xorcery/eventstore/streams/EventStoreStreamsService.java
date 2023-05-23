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
package com.exoreaction.xorcery.eventstore.streams;


import com.eventstore.dbclient.EventStoreDBClient;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.configuration.InstanceConfiguration;
import com.exoreaction.xorcery.server.api.ServiceResourceObjects;
import com.exoreaction.xorcery.server.api.ServiceResourceObject;
import com.exoreaction.xorcery.eventstore.EventStoreRels;
import com.exoreaction.xorcery.eventstore.EventStoreService;
import com.exoreaction.xorcery.reactivestreams.api.server.ReactiveStreamsServer;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

@Service
@RunLevel(6)
public class EventStoreStreamsService {

    @Inject
    public EventStoreStreamsService(EventStoreService eventStoreService,
                                    ServiceResourceObjects serviceResourceObjects,
                                    Configuration configuration,
                                    ReactiveStreamsServer reactiveStreams) {
        ObjectMapper objectMapper = new ObjectMapper()
                .findAndRegisterModules()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        ServiceResourceObject.Builder builder = new ServiceResourceObject.Builder(new InstanceConfiguration(configuration.getConfiguration("instance")), "eventstore")
                .api(EventStoreRels.eventstore.name(), "api/eventstore");

        EventStoreDBClient client = eventStoreService.getClient();

        StreamsConfiguration streamsConfiguration = new StreamsConfiguration(configuration.getConfiguration("eventstore.streams"));

        // Read
        if (streamsConfiguration.isPublisherEnabled()) {
            builder.publisher("eventstore");
            reactiveStreams.publisher("eventstore", cfg -> new EventStorePublisher(client, objectMapper, cfg), EventStorePublisher.class);
        }

        // Write
        if (streamsConfiguration.isSubscriberEnabled()) {
            builder.subscriber("eventstore");
            reactiveStreams.subscriber("eventstore", cfg -> new EventStoreSubscriber(client, cfg), EventStoreSubscriber.class);
        }

        ServiceResourceObject sro = builder.build();
        serviceResourceObjects.add(sro);
    }
}
