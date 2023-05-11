package com.exoreaction.xorcery.service.metrics;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.configuration.model.InstanceConfiguration;
import com.exoreaction.xorcery.metadata.DeploymentMetadata;
import com.exoreaction.xorcery.metadata.Metadata;
import com.exoreaction.xorcery.service.reactivestreams.api.ClientConfiguration;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreamsClient;
import jakarta.inject.Inject;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import javax.management.MBeanServer;
import java.lang.management.ManagementFactory;
import java.util.concurrent.CompletionStage;

@Service(name = "metrics")
@RunLevel(8)
public class MetricsService
        implements PreDestroy {

    private final MBeanServer managementServer;

    private final DeploymentMetadata deploymentMetadata;
    private final CompletionStage<Void> result;

    @Inject
    public MetricsService(ReactiveStreamsClient reactiveStreamsClient,
                          Configuration configuration) {

        this.managementServer = ManagementFactory.getPlatformMBeanServer();
        this.deploymentMetadata = new MetricsMetadata.Builder(new Metadata.Builder())
                .configuration(new InstanceConfiguration(configuration.getConfiguration("instance")))
                .build();

        MetricsConfiguration metricsConfiguration = new MetricsConfiguration(configuration.getConfiguration("metrics"));

        result = reactiveStreamsClient.publish(metricsConfiguration.getSubscriberAuthority(), metricsConfiguration.getSubscriberStream(),
                metricsConfiguration::getSubscriberConfiguration,
                new JmxMetricsPublisher(metricsConfiguration, deploymentMetadata, managementServer), JmxMetricsPublisher.class, new ClientConfiguration(metricsConfiguration.getPublisherConfiguration()));
    }

    @Override
    public void preDestroy() {
        result.toCompletableFuture().complete(null);
    }
}
