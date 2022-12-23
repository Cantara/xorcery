package com.exoreaction.xorcery.service.metrics;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.metadata.DeploymentMetadata;
import com.exoreaction.xorcery.metadata.Metadata;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreamsClient;
import jakarta.inject.Inject;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import javax.management.MBeanServer;
import java.lang.management.ManagementFactory;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Service(name = "metrics")
@RunLevel(8)
public class MetricsService
        implements PreDestroy {

    private final MBeanServer managementServer;
    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

    private final DeploymentMetadata deploymentMetadata;

    @Inject
    public MetricsService(ReactiveStreamsClient reactiveStreamsClient,
                          Configuration configuration) {

        this.managementServer = ManagementFactory.getPlatformMBeanServer();
        this.deploymentMetadata = new MetricsMetadata.Builder(new Metadata.Builder())
                .configuration(configuration)
                .build();

        reactiveStreamsClient.publish(configuration.getString("metrics.subscriber.authority").orElseThrow(), configuration.getString("metrics.subscriber.stream").orElseThrow(),
                () -> configuration.getConfiguration("metrics.subscriber.configuration"),
                new JmxMetricsPublisher(configuration.getConfiguration("metrics"), scheduledExecutorService, deploymentMetadata, managementServer), JmxMetricsPublisher.class, configuration.getConfiguration("metrics.publisher.configuration"));
    }

    @Override
    public void preDestroy() {
        scheduledExecutorService.shutdown();
    }
}
