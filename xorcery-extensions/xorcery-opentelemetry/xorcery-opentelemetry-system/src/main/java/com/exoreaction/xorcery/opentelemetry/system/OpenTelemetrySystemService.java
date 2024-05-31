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
package com.exoreaction.xorcery.opentelemetry.system;

import com.exoreaction.xorcery.configuration.Configuration;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.ObservableLongMeasurement;
import io.opentelemetry.semconv.SchemaUrls;
import io.opentelemetry.semconv.incubating.SystemIncubatingAttributes;
import jakarta.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service(name = "opentelemetry.instrumentations.system")
@RunLevel(0)
public class OpenTelemetrySystemService {

    private final Logger logger;
    private final List<FileStore> fileStores;

    @Inject
    public OpenTelemetrySystemService(Configuration configuration, OpenTelemetry openTelemetry, Logger logger) {
        this.logger = logger;

        OpenTelemetrySystemConfiguration systemConfiguration = OpenTelemetrySystemConfiguration.get(configuration);

        this.fileStores = getValidFileStores();

        Meter meter = openTelemetry.meterBuilder(getClass().getName())
                .setSchemaUrl(SchemaUrls.V1_25_0)
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

    private List<FileStore> getValidFileStores() {
        List<FileStore> fileStores = new ArrayList<>();
        for (FileStore fileStore : FileSystems.getDefault().getFileStores()) {
            try {
                long free = fileStore.getUnallocatedSpace();
                if (free > 0) {
                    fileStores.add(fileStore);
                }
            } catch (IOException e) {
                // Ignore file store
            }
        }
        return fileStores;
    }

    private void fileSystemUsage(ObservableLongMeasurement observableLongMeasurement) {
        for (FileStore fileStore : fileStores) {
            try {
                long total = fileStore.getTotalSpace();
                long free = fileStore.getUnallocatedSpace();
                long used = total - free;

                observableLongMeasurement.record(free,
                        Attributes.of(
                                SystemIncubatingAttributes.SYSTEM_DEVICE, fileStore.toString(),
                                SystemIncubatingAttributes.SYSTEM_FILESYSTEM_TYPE, fileStore.type().toLowerCase(),
                                SystemIncubatingAttributes.SYSTEM_FILESYSTEM_STATE, SystemIncubatingAttributes.SystemFilesystemStateValues.FREE)
                );
                observableLongMeasurement.record(used,
                        Attributes.of(
                                SystemIncubatingAttributes.SYSTEM_DEVICE, fileStore.toString(),
                                SystemIncubatingAttributes.SYSTEM_FILESYSTEM_TYPE, fileStore.type().toLowerCase(),
                                SystemIncubatingAttributes.SYSTEM_FILESYSTEM_STATE, SystemIncubatingAttributes.SystemFilesystemStateValues.USED)
                );
            } catch (IOException e) {
                logger.error("Could not record filesystem stats for "+fileStore, e);
            }
        }
    }
}
