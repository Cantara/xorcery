package com.exoreaction.xorcery.health.registry.hk2;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.health.api.HealthCheckAppInfo;
import com.exoreaction.xorcery.health.api.HealthCheckRegistry;
import com.exoreaction.xorcery.health.registry.DefaultHealthCheckService;
import com.exoreaction.xorcery.health.registry.HealthCheckService;
import jakarta.inject.Inject;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

import java.time.temporal.ChronoUnit;

@Service
@ContractsProvided({HealthCheckService.class, HealthCheckRegistry.class, DefaultHealthCheckService.class})
public class HK2DefaultHealthCheckService extends DefaultHealthCheckService {

    @Inject
    public HK2DefaultHealthCheckService(Configuration configuration, HealthCheckAppInfo appInfo) {
        super(appInfo, getMyIPAddresssString(), getMyIPAddresssesString(), new com.codahale.metrics.health.HealthCheckRegistry(), configuration.getLong("health.updater.intervalMs").orElse(1_000L), ChronoUnit.MILLIS);
    }
}
