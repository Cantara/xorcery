package com.exoreaction.xorcery.opentelemetry.system;

import com.exoreaction.xorcery.configuration.Configuration;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.ObservableLongMeasurement;
import io.opentelemetry.semconv.SemanticAttributes;
import jakarta.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.util.Collection;
import java.util.Map;

@Service(name = "opentelemetry.instrumentations.system")
@RunLevel(0)
public class OpenTelemetrySystemService {

    private final Logger logger;

    @Inject
    public OpenTelemetrySystemService(Configuration configuration, OpenTelemetry openTelemetry, Logger logger) {
        this.logger = logger;

        OpenTelemetrySystemConfiguration systemConfiguration = OpenTelemetrySystemConfiguration.get(configuration);

        Meter meter = openTelemetry.meterBuilder(getClass().getName())
                .setSchemaUrl(SemanticAttributes.SCHEMA_URL)
                .setInstrumentationVersion(getClass().getPackage().getImplementationVersion())
                .build();

        for (Map.Entry<String, String> attribute : systemConfiguration.getAttributes().entrySet()) {
            switch (attribute.getValue()) {
                // Filesystem
                case "system.filesystem.usage" ->
                        meter.upDownCounterBuilder(attribute.getKey()).setUnit("By").buildWithCallback(this::fileSystemUsage);

                default -> logger.warn("Unknown attribute {}", attribute.getValue());
            }
        }
    }

    private void fileSystemUsage(ObservableLongMeasurement observableLongMeasurement) {

        try {
            for (FileStore fileStore : FileSystems.getDefault().getFileStores()) {
                long total = fileStore.getTotalSpace();
                long free = fileStore.getUnallocatedSpace();
                long used = total - free;

                observableLongMeasurement.record(free,
                        Attributes.of(
                                SemanticAttributes.SYSTEM_DEVICE, fileStore.toString(),
                                SemanticAttributes.SYSTEM_FILESYSTEM_TYPE, fileStore.type().toLowerCase(),
                                SemanticAttributes.SYSTEM_FILESYSTEM_STATE, SemanticAttributes.SystemFilesystemStateValues.FREE)
                );
                observableLongMeasurement.record(used,
                        Attributes.of(
                                SemanticAttributes.SYSTEM_DEVICE, fileStore.toString(),
                                SemanticAttributes.SYSTEM_FILESYSTEM_TYPE, fileStore.type().toLowerCase(),
                                SemanticAttributes.SYSTEM_FILESYSTEM_STATE, SemanticAttributes.SystemFilesystemStateValues.USED)
                );
            }
        } catch (IOException e) {
            logger.error("Could not record filesystem stats", e);
        }
    }
}
