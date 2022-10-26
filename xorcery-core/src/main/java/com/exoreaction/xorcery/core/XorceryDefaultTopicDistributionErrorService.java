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
