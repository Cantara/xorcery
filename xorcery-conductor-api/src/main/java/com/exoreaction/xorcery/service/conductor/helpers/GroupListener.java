package com.exoreaction.xorcery.service.conductor.helpers;

import com.exoreaction.xorcery.service.conductor.api.Group;
import org.glassfish.hk2.api.messaging.MessageReceiver;
import org.glassfish.hk2.api.messaging.SubscribeTo;

@MessageReceiver
public interface GroupListener {
    public void group(@SubscribeTo Group group);
}
