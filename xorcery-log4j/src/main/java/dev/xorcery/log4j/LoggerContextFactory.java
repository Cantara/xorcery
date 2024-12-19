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
package dev.xorcery.log4j;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import dev.xorcery.configuration.Configuration;
import dev.xorcery.core.Xorcery;
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
import java.nio.charset.StandardCharsets;

@Service(name = "log4j2", metadata = "enabled=log4j2")
public class LoggerContextFactory
        implements Factory<LoggerContext> {

    public static LoggerContext initialize(Configuration configuration) {
        try {
            final LoggerContext loggerContext;
            String yaml = new YAMLMapper().writeValueAsString(configuration.getConfiguration("log4j2").json());
            byte[] log4j2YamlConfig = yaml.getBytes(StandardCharsets.UTF_8);
            ConfigurationSource configurationSource = new ConfigurationSource(new ByteArrayInputStream(log4j2YamlConfig));

            // Reconfigure or initialize as appropriate
            if (LogManager.getFactory().hasContext(Configurator.class.getName(), LoggerContextFactory.class.getClassLoader(), false)) {
                org.apache.logging.log4j.core.LoggerContext context = (org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(LoggerContextFactory.class.getClassLoader(), false);
                Configurator.reconfigure(new YamlConfiguration(context, configurationSource));
                loggerContext = context;
            } else {
                loggerContext = Configurator.initialize(null, new YamlConfiguration(null, configurationSource), null);
            }
            return loggerContext;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private final LoggerContext loggerContext;

    @Inject
    public LoggerContextFactory(Configuration configuration) throws IOException {
        if (configuration.getFalsy("log4j2")) {
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
