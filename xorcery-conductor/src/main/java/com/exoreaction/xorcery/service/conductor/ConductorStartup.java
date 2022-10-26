package com.exoreaction.xorcery.service.conductor;

import com.exoreaction.xorcery.core.Xorcery;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.glassfish.hk2.api.Filter;
import org.glassfish.hk2.api.InstanceLifecycleEvent;
import org.glassfish.hk2.api.InstanceLifecycleEventType;
import org.glassfish.hk2.api.InstanceLifecycleListener;
import org.jvnet.hk2.annotations.Service;

@Service
public class ConductorStartup
    implements InstanceLifecycleListener
{
    private Provider<ConductorService> conductorServiceProvider;

    @Inject
    public ConductorStartup(Provider<ConductorService> conductorServiceProvider) {
        this.conductorServiceProvider = conductorServiceProvider;
    }

    @Override
    public Filter getFilter() {
        return d -> d.getImplementation().equals(Xorcery.class.getName());
    }

    @Override
    public void lifecycleEvent(InstanceLifecycleEvent lifecycleEvent) {
        if (lifecycleEvent.getEventType().equals(InstanceLifecycleEventType.POST_PRODUCTION))
        {
            conductorServiceProvider.get().startResourceProcessing();
        }
    }
}
