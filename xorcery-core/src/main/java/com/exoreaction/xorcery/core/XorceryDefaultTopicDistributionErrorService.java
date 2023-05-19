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

import org.apache.logging.log4j.LogManager;
import org.glassfish.hk2.api.MultiException;
import org.glassfish.hk2.api.messaging.Topic;
import org.glassfish.hk2.extras.events.DefaultTopicDistributionErrorService;
import org.jvnet.hk2.annotations.Service;

@Service
public class XorceryDefaultTopicDistributionErrorService
    implements DefaultTopicDistributionErrorService
{
    @Override
    public void subscribersFailed(Topic<?> topic, Object message, MultiException error) {
        LogManager.getLogger(getClass()).error(String.format("Could not deliver message %s to topic %s", message.toString(), topic.getTopicType()), error);
    }
}
