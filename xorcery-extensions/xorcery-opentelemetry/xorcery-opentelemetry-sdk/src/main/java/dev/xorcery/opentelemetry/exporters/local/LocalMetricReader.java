package dev.xorcery.opentelemetry.exporters.local;

import dev.xorcery.configuration.Configuration;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.AggregationTemporalitySelector;
import io.opentelemetry.sdk.metrics.export.CollectionRegistration;
import io.opentelemetry.sdk.metrics.export.MetricReader;
import jakarta.inject.Inject;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

import java.time.Duration;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service(name="opentelemetry.exporters.local")
@ContractsProvided({MetricReader.class, LocalMetricReader.class})
public class LocalMetricReader
        implements MetricReader
{
    private final CompletableResultCode isShutdown = new CompletableResultCode();
    private final Configuration configuration;
    private CollectionRegistration registration;

    private final ConcurrentHashMap<String, Optional<MetricData>> currentMetricData = new ConcurrentHashMap<>();
    private Collection<MetricData> lastCollectedMetricData = null;

    @Inject
    public LocalMetricReader(Configuration configuration) {
        this.configuration = configuration;
    }

    public Optional<MetricData> getMetric(String name)
    {
        if (lastCollectedMetricData == null)
            updateMetrics();

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
            updateMetrics();
        }
        return lastCollectedMetricData;
    }

    // MetricReader implementation
    @Override
    public void register(CollectionRegistration registration) {
        this.registration = registration;
        CompletableFuture.delayedExecutor(Duration.parse("PT" + configuration.getString("opentelemetry.exporters.local.interval").orElse("10s")).getSeconds(), TimeUnit.SECONDS).execute(this::updateMetrics);
    }

    private void updateMetrics() {
        lastCollectedMetricData = registration.collectAllMetrics();
        for (MetricData collectedMetricDatum : lastCollectedMetricData) {
            if (currentMetricData.containsKey(collectedMetricDatum.getName()))
            {
                currentMetricData.put(collectedMetricDatum.getName(), Optional.of(collectedMetricDatum));
            }
        }
        if (!isShutdown.isDone())
        {
            CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS).execute(this::updateMetrics);
        }
    }

    @Override
    public CompletableResultCode forceFlush() {
        return new CompletableResultCode().succeed();
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
