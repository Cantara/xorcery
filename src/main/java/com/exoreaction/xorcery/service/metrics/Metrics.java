package com.exoreaction.xorcery.service.metrics;

import com.codahale.metrics.*;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.cqrs.metadata.DeploymentMetadata;
import com.exoreaction.xorcery.cqrs.metadata.Metadata;
import com.exoreaction.xorcery.jaxrs.AbstractFeature;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import com.exoreaction.xorcery.service.reactivestreams.api.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.ext.Provider;
import org.glassfish.jersey.server.spi.AbstractContainerLifecycleListener;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;

import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.Flow;
import java.util.regex.Pattern;

@Singleton
public class Metrics
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
                    .websocket("metricevents", "ws/metricevents");
        }

        @Override
        protected void configure() {
            context.register(Metrics.class, ContainerLifecycleListener.class);
        }
    }

    private ServiceResourceObject resourceObject;
    private final ReactiveStreams2 reactiveStreams;
    private MetricRegistry metricRegistry;

    private final DeploymentMetadata deploymentMetadata;

    @Inject
    public Metrics(@Named(SERVICE_TYPE) ServiceResourceObject resourceObject,
                   ReactiveStreams2 reactiveStreams,
                   MetricRegistry metricRegistry,
                   Configuration configuration) {
        this.resourceObject = resourceObject;
        this.reactiveStreams = reactiveStreams;
        this.metricRegistry = metricRegistry;
        this.deploymentMetadata = new MetricsMetadata.Builder(new Metadata.Builder())
                .configuration(configuration)
                .build();
    }

    @Override
    public void onStartup(Container container) {
        resourceObject.getLinkByRel("metricevents").ifPresent(link ->
        {
            reactiveStreams.publisher(link.getHrefAsUri().getPath(), cfg -> new MetricsPublisher(cfg, deploymentMetadata, metricRegistry));
        });
    }
}
