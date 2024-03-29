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
package com.exoreaction.xorcery.reactivestreams.server.providers;

import com.exoreaction.xorcery.jsonapi.Attributes;
import com.exoreaction.xorcery.reactivestreams.util.ActivePublisherSubscriptions;
import com.exoreaction.xorcery.reactivestreams.util.ActiveSubscriptions;
import com.exoreaction.xorcery.status.spi.StatusProvider;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.inject.Inject;
import org.jvnet.hk2.annotations.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * TODO: This should be moved to a separate module and added as a Flux operator
 */
@Service(name = "reactivestreams.server.status.publishers")
public class PublisherSubscriptionsStatusProvider
        implements StatusProvider {
    private final ActiveSubscriptions activeSubscriptions;

    @Inject
    public PublisherSubscriptionsStatusProvider(ActivePublisherSubscriptions activeSubscriptions) {
        this.activeSubscriptions = activeSubscriptions;
    }

    @Override
    public String getId() {
        return "publishers";
    }

    public void addAttributes(Attributes.Builder attributesBuilder, String filter) {

        Map<String, ArrayNode> streamSubscriptions = new HashMap<>();
        for (ActiveSubscriptions.ActiveSubscription activeSubscription : activeSubscriptions.getActiveSubscriptions()) {
            ArrayNode streamSubscriptionList = streamSubscriptions.computeIfAbsent(activeSubscription.stream(), name -> JsonNodeFactory.instance.arrayNode());
            ObjectNode subscriber = JsonNodeFactory.instance.objectNode();
            subscriber.set("requested", subscriber.numberNode(activeSubscription.requested().get()));
            subscriber.set("received", subscriber.numberNode(activeSubscription.received().get()));
            subscriber.set("configuration", activeSubscription.subscriptionConfiguration().json());
            streamSubscriptionList.add(subscriber);
        }

        streamSubscriptions.forEach(attributesBuilder::attribute);
    }
}
