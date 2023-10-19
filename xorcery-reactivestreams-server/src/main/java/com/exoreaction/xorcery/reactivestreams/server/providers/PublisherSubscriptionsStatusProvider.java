package com.exoreaction.xorcery.reactivestreams.server.providers;

import com.exoreaction.xorcery.jsonapi.Attributes;
import com.exoreaction.xorcery.reactivestreams.server.ActivePublisherSubscriptions;
import com.exoreaction.xorcery.reactivestreams.server.ActiveSubscriptions;
import com.exoreaction.xorcery.status.spi.StatusProvider;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.inject.Inject;
import org.jvnet.hk2.annotations.Service;

import java.util.HashMap;
import java.util.Map;

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
