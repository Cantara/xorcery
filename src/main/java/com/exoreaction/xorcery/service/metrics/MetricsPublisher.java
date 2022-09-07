package com.exoreaction.xorcery.service.metrics;

import com.codahale.metrics.MetricRegistry;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.cqrs.metadata.DeploymentMetadata;
import com.exoreaction.xorcery.service.reactivestreams.api.WithMetadata;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.Flow;
import java.util.regex.Pattern;

public class MetricsPublisher
        implements Flow.Publisher<WithMetadata<ObjectNode>> {
    private final Configuration configuration;
    private DeploymentMetadata deploymentMetadata;
    private MetricRegistry metricRegistry;

    public MetricsPublisher(Configuration publisherConfiguration,
                            DeploymentMetadata deploymentMetadata,
                            MetricRegistry metricRegistry) {
        this.configuration = publisherConfiguration;
        this.deploymentMetadata = deploymentMetadata;
        this.metricRegistry = metricRegistry;
    }


    @Override
    public void subscribe(Flow.Subscriber<? super WithMetadata<ObjectNode>> subscriber) {
        Collection<String> metricNames = new HashSet<>();
        configuration.getList("metrics").ifPresentOrElse(metrics ->
        {
            for (JsonNode metric : metrics) {
                String metricName = metric.asText();
                Pattern metricNamePattern = Pattern.compile(metricName);
                for (String name : metricRegistry.getNames()) {
                    if (metricNamePattern.matcher(name).matches()) {
                        metricNames.add(name);
                    }
                }
            }
        }, () ->
        {
            metricNames.addAll(metricRegistry.getNames());
        });
        new MetricSubscription(deploymentMetadata, subscriber, metricNames, metricRegistry);
    }
}
