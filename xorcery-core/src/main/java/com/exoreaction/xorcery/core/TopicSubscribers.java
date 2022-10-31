package com.exoreaction.xorcery.core;

import org.glassfish.hk2.api.*;
import org.glassfish.hk2.api.messaging.TopicDistributionService;
import org.glassfish.hk2.extras.events.internal.DefaultTopicDistributionService;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;

import java.util.Map;

public final class TopicSubscribers {
    public static void addSubscriber(ServiceLocator serviceLocator, Object subscriber) {
        ActiveDescriptor<?> descriptor = ServiceLocatorUtilities.addOneConstant(serviceLocator, subscriber);
        DefaultTopicDistributionService dtds = (DefaultTopicDistributionService) serviceLocator.getService(TopicDistributionService.class);
        dtds.lifecycleEvent(new InstanceLifecycleEvent() {
            @Override
            public InstanceLifecycleEventType getEventType() {
                return InstanceLifecycleEventType.POST_PRODUCTION;
            }

            @Override
            public ActiveDescriptor<?> getActiveDescriptor() {
                return descriptor;
            }

            @Override
            public Object getLifecycleObject() {
                return subscriber;
            }

            @Override
            public Map<Injectee, Object> getKnownInjectees() {
                return null;
            }
        });
    }
}
