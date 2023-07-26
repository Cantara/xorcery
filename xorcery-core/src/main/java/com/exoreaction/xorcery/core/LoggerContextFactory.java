package com.exoreaction.xorcery.core;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.util.Resources;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.yaml.YamlConfiguration;
import org.apache.logging.log4j.spi.LoggerContext;
import org.glassfish.hk2.api.Factory;
import org.jvnet.hk2.annotations.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@Service(name = "log4j2", metadata = "enabled=log4j2")
public class LoggerContextFactory
        implements Factory<LoggerContext> {

    public static LoggerContext initialize(Configuration configuration) {
        try {
            final LoggerContext loggerContext;
            String yaml = new YAMLMapper().writeValueAsString(configuration.getConfiguration("log4j2").json());
            byte[] log4j2YamlConfig = yaml.getBytes(StandardCharsets.UTF_8);
            URL baseConfigUrl = Resources.getResource("META-INF/xorcery-defaults.yaml").orElseThrow();
            ConfigurationSource configurationSource = new ConfigurationSource(new ByteArrayInputStream(log4j2YamlConfig), baseConfigUrl);

            // Reconfigure or initialize as appropriate
            if (LogManager.getFactory().hasContext(Configurator.class.getName(), LoggerContextFactory.class.getClassLoader(), false)) {
                org.apache.logging.log4j.core.LoggerContext context = (org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(LoggerContextFactory.class.getClassLoader(), false);
                Configurator.reconfigure(new YamlConfiguration(context, configurationSource));
                loggerContext = context;
            } else {
                loggerContext = Configurator.initialize(null, configurationSource, null);
            }
            return loggerContext;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
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
