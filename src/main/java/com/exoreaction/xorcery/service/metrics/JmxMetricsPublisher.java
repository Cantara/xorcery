package com.exoreaction.xorcery.service.metrics;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.metadata.DeploymentMetadata;
import com.exoreaction.xorcery.service.reactivestreams.api.WithMetadata;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javax.management.MBeanServer;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Flow;
import java.util.concurrent.ScheduledExecutorService;

public class JmxMetricsPublisher
        implements Flow.Publisher<WithMetadata<ObjectNode>> {
    private final Configuration configuration;
    private ScheduledExecutorService scheduledExecutorService;
    private DeploymentMetadata deploymentMetadata;
    private MBeanServer managementServer;

    public JmxMetricsPublisher(Configuration publisherConfiguration,
                               ScheduledExecutorService scheduledExecutorService,
                               DeploymentMetadata deploymentMetadata,
                               MBeanServer managementServer) {
        this.configuration = publisherConfiguration;
        this.scheduledExecutorService = scheduledExecutorService;
        this.deploymentMetadata = deploymentMetadata;
        this.managementServer = managementServer;
    }


    @Override
    public void subscribe(Flow.Subscriber<? super WithMetadata<ObjectNode>> subscriber) {
        Optional<List<String>> filters = configuration.getList("filter").map(list ->
        {
            List<String> f = new ArrayList<>();
            for (JsonNode jsonNode : list) {
                f.add(jsonNode.asText());
            }
            return f;
        });
        new JmxMetricSubscription(configuration.getString("delay").map(Duration::parse).orElse(Duration.ofSeconds(30)), scheduledExecutorService, deploymentMetadata, subscriber, filters, managementServer);
    }
}
