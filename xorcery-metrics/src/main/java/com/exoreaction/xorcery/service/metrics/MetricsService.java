package com.exoreaction.xorcery.service.metrics;

import com.codahale.metrics.MetricRegistry;
import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.metadata.DeploymentMetadata;
import com.exoreaction.xorcery.metadata.Metadata;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import com.exoreaction.xorcery.service.conductor.helpers.ClientPublisherGroupListener;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreams;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.messaging.Topic;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;

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
    public MetricsService(Topic<ServiceResourceObject> registryTopic,
                          ReactiveStreams reactiveStreams,
                          ServiceLocator serviceLocator,
                          MetricRegistry metricRegistry,
                          Configuration configuration) {
        this.resourceObject = new ServiceResourceObject.Builder(() -> configuration, SERVICE_TYPE)
                .websocket("metricevents", "ws/metricevents")
                .websocket("jmxmetrics", "ws/jmxmetrics")
                .build();

        this.managementServer = ManagementFactory.getPlatformMBeanServer();
        this.deploymentMetadata = new MetricsMetadata.Builder(new Metadata.Builder())
                .configuration(configuration)
                .build();

        resourceObject.getLinkByRel("jmxmetrics").ifPresent(link ->
        {
            reactiveStreams.publisher(link.getHrefAsUri().getPath(), cfg -> new JmxMetricsPublisher(cfg, scheduledExecutorService, deploymentMetadata, managementServer), JmxMetricsPublisher.class);
        });
        resourceObject.getLinkByRel("metricevents").ifPresent(link ->
        {
            reactiveStreams.publisher(link.getHrefAsUri().getPath(), cfg -> new MetricsPublisher(cfg, deploymentMetadata, metricRegistry), MetricsPublisher.class);
        });
        ServiceLocatorUtilities.addOneConstant(serviceLocator, new ClientPublisherGroupListener(resourceObject.getServiceIdentifier(), cfg -> new JmxMetricsPublisher(cfg, scheduledExecutorService, deploymentMetadata, managementServer), JmxMetricsPublisher.class, null, reactiveStreams));

        registryTopic.publish(resourceObject);
    }

    @Override
    public void preDestroy() {
        scheduledExecutorService.shutdown();
    }
}
