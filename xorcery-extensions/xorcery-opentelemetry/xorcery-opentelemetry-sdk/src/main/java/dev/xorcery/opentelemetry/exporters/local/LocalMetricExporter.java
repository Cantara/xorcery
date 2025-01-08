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
package dev.xorcery.opentelemetry.exporters.local;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.AggregationTemporalitySelector;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import jakarta.inject.Inject;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service(name="opentelemetry.exporters.local")
@ContractsProvided({MetricExporter.class, LocalMetricExporter.class})
public class LocalMetricExporter
        implements MetricExporter
{
    private final CompletableResultCode isShutdown = new CompletableResultCode();

    private final ConcurrentHashMap<String, Optional<MetricData>> currentMetricData = new ConcurrentHashMap<>();
    private Collection<MetricData> lastCollectedMetricData = null;

    private final Object updateNotification = new Object();

    @Inject
    public LocalMetricExporter() {
    }

    public Optional<MetricData> getMetric(String name)
    {
        synchronized (updateNotification){
            try {
                updateNotification.wait(30000);
            } catch (InterruptedException e) {
                return Optional.empty();
            }
        }

        return currentMetricData.computeIfAbsent(name, n ->
        {
            if (lastCollectedMetricData != null)
            {
                for (MetricData lastCollectedMetricDatum : lastCollectedMetricData) {
                    if (lastCollectedMetricDatum.getName().equals(n))
                    {
                        return Optional.of(lastCollectedMetricDatum);
                    }
                }
            }
            return Optional.empty();
        });
    }

    public Collection<MetricData> getMetrics()
    {
        if (lastCollectedMetricData == null)
        {
            synchronized (updateNotification){
                try {
                    updateNotification.wait(30000);
                } catch (InterruptedException e) {
                    return Collections.emptyList();
                }
            }
        }
        return lastCollectedMetricData;
    }

    // MetricExporter implementation
    @Override
    public CompletableResultCode export(Collection<MetricData> metrics) {
        lastCollectedMetricData = metrics;
        for (MetricData collectedMetricDatum : lastCollectedMetricData) {
            if (currentMetricData.containsKey(collectedMetricDatum.getName()))
            {
                currentMetricData.put(collectedMetricDatum.getName(), Optional.of(collectedMetricDatum));
            }
        }
        synchronized (updateNotification){
            updateNotification.notifyAll();
        }
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode flush() {
        return CompletableResultCode.ofSuccess();
    }
    @Override
    public CompletableResultCode shutdown() {
        return isShutdown.succeed();
    }

    @Override
    public AggregationTemporality getAggregationTemporality(InstrumentType instrumentType) {
        return AggregationTemporalitySelector.alwaysCumulative().getAggregationTemporality(instrumentType);
    }
}
