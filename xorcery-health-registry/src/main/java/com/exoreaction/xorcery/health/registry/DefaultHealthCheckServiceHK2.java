package com.exoreaction.xorcery.health.registry;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.health.api.HealthCheckRegistry;
import jakarta.inject.Inject;
import org.glassfish.hk2.api.PreDestroy;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

@Service
@ContractsProvided({HealthCheckService.class, HealthCheckRegistry.class, DefaultHealthCheckService.class, PreDestroy.class})
public class DefaultHealthCheckServiceHK2
        extends DefaultHealthCheckService
implements PreDestroy
{

    @Inject
    public DefaultHealthCheckServiceHK2(Configuration configuration) {
        super(configuration, new com.codahale.metrics.health.HealthCheckRegistry());
    }

    @Override
    public void preDestroy() {
        shutdown();
    }
}
