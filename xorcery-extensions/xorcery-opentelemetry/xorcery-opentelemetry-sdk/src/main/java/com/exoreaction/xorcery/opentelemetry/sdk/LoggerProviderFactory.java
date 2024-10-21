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
package com.exoreaction.xorcery.opentelemetry.sdk;

import io.opentelemetry.sdk.logs.LogRecordProcessor;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.resources.Resource;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.IterableProvider;
import org.jvnet.hk2.annotations.Service;

@Service(name = "opentelemetry")
public class LoggerProviderFactory
        implements Factory<SdkLoggerProvider> {
    private final SdkLoggerProvider sdkLoggerProvider;

    @Inject
    public LoggerProviderFactory(Resource resource,
                                 IterableProvider<LogRecordProcessor> logRecordProcessors) {
        var sdkLoggerProviderBuilder = SdkLoggerProvider.builder()
                .setResource(resource);
        for (LogRecordProcessor logRecordProcessor : logRecordProcessors) {
            sdkLoggerProviderBuilder.addLogRecordProcessor(logRecordProcessor);
        }
        sdkLoggerProvider = sdkLoggerProviderBuilder.build();
    }

    @Override
    @Singleton
    public SdkLoggerProvider provide() {
        return sdkLoggerProvider;
    }

    @Override
    public void dispose(SdkLoggerProvider instance) {
        sdkLoggerProvider.close();
    }
}
