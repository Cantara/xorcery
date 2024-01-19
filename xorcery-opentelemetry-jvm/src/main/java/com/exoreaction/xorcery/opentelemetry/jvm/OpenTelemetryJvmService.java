package com.exoreaction.xorcery.opentelemetry.jvm;

import com.exoreaction.xorcery.configuration.Configuration;
import com.sun.management.GarbageCollectionNotificationInfo;
import com.sun.management.OperatingSystemMXBean;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.ObservableDoubleMeasurement;
import io.opentelemetry.api.metrics.ObservableLongMeasurement;
import io.opentelemetry.semconv.SemanticAttributes;
import jakarta.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import javax.management.InstanceNotFoundException;
import javax.management.ListenerNotFoundException;
import javax.management.NotificationFilterSupport;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

@Service(name = "opentelemetry.instrumentations.jvm")
@RunLevel(0)
public class OpenTelemetryJvmService
        implements PreDestroy {

    private final NotificationListener gcNotificationListener;
    private final Map<MemoryPoolMXBean, Attributes> memoryPoolAttributes;

    @Inject
    public OpenTelemetryJvmService(Configuration configuration, OpenTelemetry openTelemetry) throws InstanceNotFoundException {

        OpenTelemetryJvmConfiguration jvmConfiguration = OpenTelemetryJvmConfiguration.get(configuration);

        Meter meter = openTelemetry.meterBuilder(getClass().getName())
                .setSchemaUrl(SemanticAttributes.SCHEMA_URL)
                .setInstrumentationVersion(getClass().getPackage().getImplementationVersion())
                .build();

        // Memory Pools
        memoryPoolAttributes = ManagementFactory.getMemoryPoolMXBeans().stream()
                .map(pool -> Map.entry(pool, Attributes.builder()
                        .put(SemanticAttributes.JVM_MEMORY_POOL_NAME, pool.getName())
                        .put(SemanticAttributes.JVM_MEMORY_TYPE, pool.getType().name().toLowerCase())
                        .build()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        // GC
        gcNotificationListener = (notification, handback) -> {
            String notifType = notification.getType();
            if (handback instanceof DoubleHistogram dh) {
                if (notifType.equals(GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION)) {
                    CompositeData cd = (CompositeData) notification.getUserData();
                    GarbageCollectionNotificationInfo info = GarbageCollectionNotificationInfo.from(cd);

                    Attributes attributes = Attributes.builder()
                            .put(SemanticAttributes.JVM_GC_ACTION, info.getGcAction())
                            .put(SemanticAttributes.JVM_GC_NAME, info.getGcName())
                            .build();
                    dh.record(info.getGcInfo().getDuration() / 1000D, attributes);
                }
            }
        };

        for (Map.Entry<String, String> attribute : jvmConfiguration.getAttributes().entrySet()) {
            switch (attribute.getValue())
            {
                case "jvm.memory.used" -> meter.upDownCounterBuilder(attribute.getKey()).setUnit("By").buildWithCallback(this::jvmMemoryUsed);
                case "jvm.memory.committed"->meter.upDownCounterBuilder(attribute.getKey()).setUnit("By").buildWithCallback(this::jvmMemoryCommitted);
                case "jvm.memory.limit"-> meter.upDownCounterBuilder(attribute.getKey()).setUnit("By").buildWithCallback(this::jvmMemoryLimit);
                case "jvm.memory.used_after_last_gc" -> meter.upDownCounterBuilder(attribute.getKey()).setUnit("By").buildWithCallback(this::jvmMemoryUserAfterLastGC);
                case "jvm.gc.duration" ->
                {
                    for (GarbageCollectorMXBean garbageCollectorMXBean : ManagementFactory.getGarbageCollectorMXBeans()) {
                        DoubleHistogram gcHistogram = meter.histogramBuilder(attribute.getKey())
                                .setUnit("s")
                                .setExplicitBucketBoundariesAdvice(List.of(0.01D, 0.1D, 1D, 10D))
                                .build();
                        ManagementFactory.getPlatformMBeanServer().addNotificationListener(garbageCollectorMXBean.getObjectName(), gcNotificationListener, new NotificationFilterSupport(), gcHistogram);
                    }
                }

                // Threads
                case "jvm.thread.count" -> meter.upDownCounterBuilder(attribute.getKey()).setUnit("{thread}").buildWithCallback(this::jvmThreadCount);

                // CPU
                case "jvm.cpu.time" -> meter.counterBuilder(attribute.getKey()).setUnit("s").ofDoubles().buildWithCallback(this::jvmCpuTime);
                case "jvm.cpu.count" -> meter.upDownCounterBuilder(attribute.getKey()).setUnit("{cpu}").build().add(Runtime.getRuntime().availableProcessors());
                case "jvm.cpu.recent_utilization" -> meter.gaugeBuilder(attribute.getKey()).setUnit("1").buildWithCallback(this::jvmCpuRecentUtilization);
            }
        }
    }

    @Override
    public void preDestroy() {
        for (GarbageCollectorMXBean garbageCollectorMXBean : ManagementFactory.getGarbageCollectorMXBeans()) {
            try {
                ManagementFactory.getPlatformMBeanServer().removeNotificationListener(garbageCollectorMXBean.getObjectName(), gcNotificationListener);
            } catch (ListenerNotFoundException e) {
                // Ok!
            }catch (Throwable e) {
                LogManager.getLogger().error("Could not remove notification listener", e);
            }
        }
    }

    private void jvmMemoryUsed(ObservableLongMeasurement observableLongMeasurement) {
        memoryPoolAttributes.forEach((pool, attributes) -> observableLongMeasurement.record(pool.getUsage().getUsed(), attributes));
    }

    private void jvmMemoryCommitted(ObservableLongMeasurement observableLongMeasurement) {
        memoryPoolAttributes.forEach((pool, attributes) -> observableLongMeasurement.record(pool.getUsage().getCommitted(), attributes));
    }

    private void jvmMemoryLimit(ObservableLongMeasurement observableLongMeasurement) {
        memoryPoolAttributes.forEach((pool, attributes) -> observableLongMeasurement.record(pool.getUsage().getMax(), attributes));
    }

    private void jvmMemoryUserAfterLastGC(ObservableLongMeasurement observableLongMeasurement) {
        memoryPoolAttributes.forEach((pool, attributes) ->
                ofNullable(pool.getCollectionUsage()).ifPresent(mu ->
                        observableLongMeasurement.record(mu.getUsed(), attributes)));
    }

    private void jvmThreadCount(ObservableLongMeasurement observableLongMeasurement) {
        observableLongMeasurement.record(ManagementFactory.getThreadMXBean().getThreadCount());
    }

    private void jvmCpuTime(ObservableDoubleMeasurement observableDoubleMeasurement) {
        if (ManagementFactory.getOperatingSystemMXBean() instanceof OperatingSystemMXBean osmxbean) {
            observableDoubleMeasurement.record(osmxbean.getProcessCpuTime());
        }
    }

    private void jvmCpuRecentUtilization(ObservableDoubleMeasurement observableDoubleMeasurement) {
        if (ManagementFactory.getOperatingSystemMXBean() instanceof OperatingSystemMXBean osmxbean) {
            observableDoubleMeasurement.record(osmxbean.getProcessCpuLoad());
        }
    }
}
