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
package dev.xorcery.opentelemetry.exporters.otlphttp;

import dev.xorcery.configuration.Configuration;
import dev.xorcery.secrets.Secrets;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporterBuilder;
import io.opentelemetry.sdk.common.export.RetryPolicy;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.glassfish.hk2.api.Factory;
import org.jvnet.hk2.annotations.Service;

@Service(name="opentelemetry.exporters.otlp.http")
public class OtlpHttpLogRecordExporterFactory
        implements Factory<OtlpHttpLogRecordExporter> {
    private final OtlpHttpLogRecordExporter logRecordExporter;

    @Inject
    public OtlpHttpLogRecordExporterFactory(Configuration configuration,
                                            Secrets secrets,
                                            SdkMeterProvider meterProvider) {
        OtlpHttpConfiguration otHttpConfiguration = OtlpHttpConfiguration.get(configuration);

        OtlpHttpLogRecordExporterBuilder builder = OtlpHttpLogRecordExporter.builder();
        builder.setEndpoint(otHttpConfiguration.getLogsEndpoint());
        builder.setConnectTimeout(otHttpConfiguration.getConnectTimeout());
        builder.setTimeout(otHttpConfiguration.getTimeout());
        builder.setRetryPolicy(RetryPolicy.getDefault());
        otHttpConfiguration.getHeaders(secrets).forEach(builder::addHeader);
        otHttpConfiguration.getCompression().ifPresent(builder::setCompression);
        builder.setMeterProvider(meterProvider);
        logRecordExporter = builder.build();
    }

    @Override
    @Singleton
    public OtlpHttpLogRecordExporter provide() {
        return logRecordExporter;
    }

    @Override
    public void dispose(OtlpHttpLogRecordExporter instance) {
    }
}
