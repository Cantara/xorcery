package com.exoreaction.xorcery.service.metrics;

import com.codahale.metrics.MetricRegistry;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.cqrs.metadata.DeploymentMetadata;
import com.exoreaction.xorcery.cqrs.metadata.Metadata;
import com.exoreaction.xorcery.jaxrs.AbstractFeature;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import com.exoreaction.xorcery.service.conductor.api.Conductor;
import com.exoreaction.xorcery.service.reactivestreams.api.ReactiveStreams;
import com.exoreaction.xorcery.service.reactivestreams.helper.ClientPublisherConductorListener;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.ext.Provider;
import org.glassfish.jersey.server.spi.AbstractContainerLifecycleListener;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;

import javax.management.MBeanServer;
import java.lang.management.ManagementFactory;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Singleton
public class MetricsService
        extends AbstractContainerLifecycleListener {

    public static final String SERVICE_TYPE = "metrics";

    @Provider
    public static class Feature
            extends AbstractFeature {

        @Override
        protected String serviceType() {
            return SERVICE_TYPE;
        }

        @Override
        protected void buildResourceObject(ServiceResourceObject.Builder builder) {
            builder.api("metrics", "api/metrics")
                    .websocket("metricevents", "ws/metricevents")
                    .websocket("jmxmetrics", "ws/jmxmetrics");
        }

        @Override
        protected void configure() {
            context.register(MetricsService.class, ContainerLifecycleListener.class);
        }
    }

    private ServiceResourceObject resourceObject;
    private final ReactiveStreams reactiveStreams;
    private Conductor conductor;
    private MetricRegistry metricRegistry;
    private final MBeanServer managementServer;
    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();


    private final DeploymentMetadata deploymentMetadata;

    @Inject
    public MetricsService(@Named(SERVICE_TYPE) ServiceResourceObject resourceObject,
                          ReactiveStreams reactiveStreams,
                          Conductor conductor,
                          MetricRegistry metricRegistry,
                          Configuration configuration) {
        this.resourceObject = resourceObject;
        this.reactiveStreams = reactiveStreams;
        this.conductor = conductor;
        this.metricRegistry = metricRegistry;
        this.managementServer = ManagementFactory.getPlatformMBeanServer();
        this.deploymentMetadata = new MetricsMetadata.Builder(new Metadata.Builder())
                .configuration(configuration)
                .build();
    }

    @Override
    public void onStartup(Container container) {
        resourceObject.getLinkByRel("metricevents").ifPresent(link ->
        {
            reactiveStreams.publisher(link.getHrefAsUri().getPath(), cfg -> new MetricsPublisher(cfg, deploymentMetadata, metricRegistry), MetricsPublisher.class);
        });
//        conductor.addConductorListener(new ClientPublisherConductorListener(resourceObject.serviceIdentifier(), cfg -> new MetricsPublisher(cfg, deploymentMetadata, metricRegistry), MetricsPublisher.class, null, reactiveStreams));

        conductor.addConductorListener(new ClientPublisherConductorListener(resourceObject.serviceIdentifier(), cfg -> new JmxMetricsPublisher(cfg, scheduledExecutorService, deploymentMetadata, managementServer), JmxMetricsPublisher.class, null, reactiveStreams));

    }

    @Override
    public void onShutdown(Container container) {
        scheduledExecutorService.shutdown();
    }
}
