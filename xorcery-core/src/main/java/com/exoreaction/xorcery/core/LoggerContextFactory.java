package com.exoreaction.xorcery.core;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.util.Resources;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.spi.LoggerContext;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.Injectee;
import org.glassfish.hk2.api.InstantiationService;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@Service
public class LoggerContextFactory
        implements Factory<LoggerContext> {

    public static LoggerContext initialize(Configuration configuration) {
        final LoggerContext loggerContext;
        byte[] log4j2YamlConfig = configuration.getConfiguration("log4j2").json().toString().getBytes(StandardCharsets.UTF_8);
        URL baseConfigUrl = Resources.getResource("META-INF/xorcery-defaults.yaml").orElseThrow();
        ConfigurationSource configurationSource = new ConfigurationSource(new ByteArrayInputStream(log4j2YamlConfig), baseConfigUrl);
        loggerContext = Configurator.initialize(Xorcery.class.getClassLoader(), configurationSource, configuration.getString("instance.id").orElse("xorcery"));
        return loggerContext;
    }

    private final LoggerContext loggerContext;

    @Inject
    public LoggerContextFactory(Configuration configuration) throws IOException {
        if (configuration.getFalsy("log4j2").orElse(false)) {
            loggerContext = initialize(configuration);
        } else {
            loggerContext = LogManager.getContext(Xorcery.class.getClassLoader(), false);
        }
    }

    @Override
    @Singleton
    public LoggerContext provide() {
        return loggerContext;
    }

    @Override
    public void dispose(LoggerContext instance) {
    }
}
