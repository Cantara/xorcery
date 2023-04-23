package com.exoreaction.xorcery.health.registry.hk2;

import com.codahale.metrics.health.HealthCheckRegistry;
import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.health.api.XorceryHealthCheckRegistry;
import com.exoreaction.xorcery.health.registry.DefaultXorceryHealthCheckService;
import com.exoreaction.xorcery.health.registry.XorceryHealthCheckService;
import jakarta.inject.Inject;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

import java.time.temporal.ChronoUnit;

@Service
@ContractsProvided({XorceryHealthCheckService.class, XorceryHealthCheckRegistry.class, DefaultXorceryHealthCheckService.class})
public class HK2DefaultXorceryHealthCheckService extends DefaultXorceryHealthCheckService {

    @Inject
    public HK2DefaultXorceryHealthCheckService(Configuration configuration) {
        super(readMetaInfMavenPomVersion(configuration.getString("maven.groupId").orElseThrow(), configuration.getString("maven.artifactId").orElseThrow()), getMyIPAddresssString(), getMyIPAddresssesString(), new HealthCheckRegistry(), configuration.getLong("health.updater.intervalMs").orElse(1_000L), ChronoUnit.MILLIS);
    }
}
