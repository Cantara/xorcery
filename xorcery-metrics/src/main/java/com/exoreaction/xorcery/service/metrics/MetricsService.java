package com.exoreaction.xorcery.service.metrics;

import com.codahale.metrics.MetricRegistry;
import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.metadata.DeploymentMetadata;
import com.exoreaction.xorcery.metadata.Metadata;
import com.exoreaction.xorcery.server.api.ServiceResourceObjects;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreamsServer;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.hk2.api.ServiceLocator;

import javax.management.MBeanServer;
import java.lang.management.ManagementFactory;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Singleton
public class MetricsService
        implements PreDestroy {

    public static final String SERVICE_TYPE = "metrics";

    private final ServiceResourceObject resourceObject;
    private final MBeanServer managementServer;
    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();


    private final DeploymentMetadata deploymentMetadata;

    @Inject
    public MetricsService(ServiceResourceObjects serviceResourceObjects,
                          ReactiveStreamsServer reactiveStreams,
                          Configuration configuration) {
        this.resourceObject = new ServiceResourceObject.Builder(() -> configuration, SERVICE_TYPE)
                .publisher("metrics")
                .build();

        this.managementServer = ManagementFactory.getPlatformMBeanServer();
        this.deploymentMetadata = new MetricsMetadata.Builder(new Metadata.Builder())
                .configuration(configuration)
                .build();

        reactiveStreams.publisher("metrics", cfg -> new JmxMetricsPublisher(cfg, scheduledExecutorService, deploymentMetadata, managementServer), JmxMetricsPublisher.class);
        serviceResourceObjects.publish(resourceObject);
    }

    @Override
    public void preDestroy() {
        scheduledExecutorService.shutdown();
    }
}
