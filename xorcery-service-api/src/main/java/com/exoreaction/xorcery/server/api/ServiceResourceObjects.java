package com.exoreaction.xorcery.server.api;

import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import jakarta.inject.Inject;
import org.glassfish.hk2.api.Filter;
import org.glassfish.hk2.api.InstanceLifecycleEvent;
import org.glassfish.hk2.api.InstanceLifecycleEventType;
import org.glassfish.hk2.api.InstanceLifecycleListener;
import org.glassfish.hk2.api.messaging.Topic;
import org.jvnet.hk2.annotations.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Collection of ServiceResourceObjects for this server. Each service should add their own ServiceResourceObject instances
 * here on startup. This will be used by the Registry and Conductor.
 */
@Service
public class ServiceResourceObjects
        implements InstanceLifecycleListener {
    private List<ServiceResourceObject> serviceResources = new ArrayList<>();
    private Topic<ServiceResourceObject> serviceResourceObjectTopic;
    private boolean started = false;

    @Inject
    public ServiceResourceObjects(Topic<ServiceResourceObject> serviceResourceObjectTopic) {
        this.serviceResourceObjectTopic = serviceResourceObjectTopic;
    }

    public void publish(ServiceResourceObject serviceResourceObject) {
        add(serviceResourceObject);
    }

    public void add(ServiceResourceObject serviceResourceObject) {
        serviceResources.add(serviceResourceObject);
        if (started)
        {
            serviceResourceObjectTopic.publish(serviceResourceObject);
        }
    }

    public List<ServiceResourceObject> getServiceResources() {
        return serviceResources;
    }

    @Override
    public Filter getFilter() {
        return d -> d.getImplementation().equals("com.exoreaction.xorcery.core.Xorcery");
    }

    @Override
    public void lifecycleEvent(InstanceLifecycleEvent lifecycleEvent) {
        if (lifecycleEvent.getEventType().equals(InstanceLifecycleEventType.POST_PRODUCTION)) {
            for (ServiceResourceObject serviceResource : serviceResources) {
                serviceResourceObjectTopic.publish(serviceResource);
            }
            started = true;
        }
    }
}
