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
package com.exoreaction.xorcery.metricregistry;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jmx.JmxReporter;
import jakarta.inject.Inject;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.Injectee;
import org.glassfish.hk2.api.InstantiationService;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MetricRegistryFactory
        implements Factory<MetricRegistry> {
    private final MetricRegistry metricRegistry = new MetricRegistry();
    private final Map<String, MetricRegistry> prefixedRegistries = new ConcurrentHashMap<>();
    private final InstantiationService instantiationService;
    private final JmxReporter reporter;

    @Inject
    public MetricRegistryFactory(InstantiationService instantiationService) {
        this.instantiationService = instantiationService;

        reporter = JmxReporter.forRegistry(metricRegistry)
                .inDomain("xorcery")
                .build();
        reporter.start();
    }

    @Override
    @PerLookup
    public MetricRegistry provide() {

        Injectee injectee = instantiationService.getInstantiationData().getParentInjectee();
        if (injectee != null && injectee.getInjecteeDescriptor() != null) {
            String name = injectee.getInjecteeDescriptor().getName();
            if (name != null) {
                return prefixedRegistries.computeIfAbsent(name, n ->
                {
                    MetricRegistry prefixedRegistry = new MetricRegistry();
                    metricRegistry.register(n, prefixedRegistry);
                    return prefixedRegistry;
                });
            }
        }

        return metricRegistry;
    }

    @Override
    public void dispose(MetricRegistry instance) {
        reporter.stop();
    }
}
