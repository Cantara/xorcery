package com.exoreaction.xorcery.opentelemetry.sdk.exporters;

import io.opentelemetry.sdk.logs.LogRecordProcessor;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.resources.Resource;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.IterableProvider;
import org.jvnet.hk2.annotations.Service;

@Service(name = "opentelemetry.exporters")
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
