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
package com.exoreaction.xorcery.metrics;

import com.exoreaction.xorcery.metadata.DeploymentMetadata;
import com.exoreaction.xorcery.reactivestreams.api.WithMetadata;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import javax.management.MBeanServer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JmxMetricsPublisher
        implements Publisher<WithMetadata<ObjectNode>> {
    private final MetricsConfiguration configuration;
    private DeploymentMetadata deploymentMetadata;
    private MBeanServer managementServer;

    public JmxMetricsPublisher(MetricsConfiguration metricsConfiguration,
                               DeploymentMetadata deploymentMetadata,
                               MBeanServer managementServer) {
        this.configuration = metricsConfiguration;
        this.deploymentMetadata = deploymentMetadata;
        this.managementServer = managementServer;
    }


    @Override
    public void subscribe(Subscriber<? super WithMetadata<ObjectNode>> subscriber) {
        Optional<List<String>> filters = configuration.getFilters().map(list ->
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
