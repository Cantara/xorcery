package com.exoreaction.xorcery.service.metrics;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.metadata.DeploymentMetadata;
import com.exoreaction.xorcery.service.reactivestreams.api.WithMetadata;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javax.management.MBeanServer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Flow;

public class JmxMetricsPublisher
        implements Flow.Publisher<WithMetadata<ObjectNode>> {
    private final Configuration configuration;
    private DeploymentMetadata deploymentMetadata;
    private MBeanServer managementServer;

    public JmxMetricsPublisher(Configuration publisherConfiguration,
                               DeploymentMetadata deploymentMetadata,
                               MBeanServer managementServer) {
        this.configuration = publisherConfiguration;
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
        new JmxMetricSubscription(deploymentMetadata, subscriber, filters, managementServer);
    }
}
