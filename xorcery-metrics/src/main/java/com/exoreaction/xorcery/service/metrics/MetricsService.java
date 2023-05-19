/*
 * Copyright © 2022 eXOReaction AS (rickard@exoreaction.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
