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
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Collection of ServiceResourceObjects for this server. Each service should add their own ServiceResourceObject instances
 * here on startup. This will get exposed by the API for the root of the JSON API so that clients can inspect what services are available.
 */
@Service
public class ServiceResourceObjects {
    private List<ServiceResourceObject> serviceResources = new CopyOnWriteArrayList<>();

    @Inject
    public ServiceResourceObjects() {
    }

    public void add(ServiceResourceObject serviceResourceObject) {
        serviceResources.add(serviceResourceObject);
    }

    public List<ServiceResourceObject> getServiceResources() {
        return serviceResources;
    }
}
