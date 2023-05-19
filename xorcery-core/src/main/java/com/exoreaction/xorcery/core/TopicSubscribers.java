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
