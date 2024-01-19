package com.exoreaction.xorcery.opentelemetry.log4j;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.log4j.appender.v2_17.OpenTelemetryAppender;
import jakarta.inject.Inject;
import org.apache.logging.log4j.spi.LoggerContext;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

@Service(name = "opentelemetry.log4j2", metadata = "enabled=log4j2")
@RunLevel(0)
public class OpenTelemetryLog4j2Installer {
    @Inject
    public OpenTelemetryLog4j2Installer(OpenTelemetry openTelemetry,
                                        LoggerContext loggerContextSpi) {

        // Install into Log4j2
        org.apache.logging.log4j.core.LoggerContext loggerContext = (org.apache.logging.log4j.core.LoggerContext) loggerContextSpi;
        org.apache.logging.log4j.core.config.Configuration config = loggerContext.getConfiguration();
        config
                .getAppenders()
                .values()
                .forEach(
                        appender -> {
                            if (appender instanceof OpenTelemetryAppender) {
                                ((OpenTelemetryAppender) appender).setOpenTelemetry(openTelemetry);
                            }
                        });
    }
}
