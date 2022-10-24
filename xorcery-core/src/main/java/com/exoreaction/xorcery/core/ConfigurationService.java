package com.exoreaction.xorcery.core;

import com.exoreaction.xorcery.configuration.builder.StandardConfigurationBuilder;
import com.exoreaction.xorcery.configuration.model.Configuration;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.Rank;
import org.jvnet.hk2.annotations.Service;

import java.io.File;

@Service
public class ConfigurationService
        implements Factory<Configuration> {
    private final Logger logger = LogManager.getLogger(getClass());
    private final Provider<File> configFile;

    @Inject
    public ConfigurationService(@Named("configuration") Provider<File> configFile) throws JsonProcessingException {
        this.configFile = configFile;
    }

    @Override
    @Singleton
    @Rank(-1)
    public Configuration provide() {
        try {
            StandardConfigurationBuilder standardConfigurationBuilder = new StandardConfigurationBuilder();
            Configuration.Builder builder = new Configuration.Builder()
                    .with(standardConfigurationBuilder::addDefaults, standardConfigurationBuilder.addFile(configFile.get()));

            // Log final configuration
            ObjectWriter objectWriter = new ObjectMapper(new YAMLFactory()).writer().withDefaultPrettyPrinter();
            logger.debug("Configuration:\n" + objectWriter.writeValueAsString(builder.builder()));

            Configuration configuration = builder.build();
            logger.info("Resolved configuration:\n" + objectWriter.writeValueAsString(configuration.json()));
            return configuration;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void dispose(Configuration instance) {

    }
}
