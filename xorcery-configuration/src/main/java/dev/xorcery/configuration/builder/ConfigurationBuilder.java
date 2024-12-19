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
package dev.xorcery.configuration.builder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import dev.xorcery.configuration.Configuration;
import dev.xorcery.configuration.ConfigurationLogger;
import dev.xorcery.configuration.spi.ConfigurationProvider;
import dev.xorcery.json.JsonMerger;
import dev.xorcery.util.Resources;

import java.io.*;
import java.net.URL;
import java.util.ServiceLoader;
import java.util.function.Consumer;

/**
 * Configuration builder for the most common cases. If you need anything more than this, use the Configuration.Builder and StandardConfigurationBuilder directly.
 *
 * @param builder
 * @param baseName
 */
public record ConfigurationBuilder(Configuration.Builder builder, String baseName)
{
    private static final ConfigurationLogger logger = ConfigurationLogger.getLogger();
    private static final YAMLMapper yamlMapper = new YAMLMapper();
    private static final JavaPropsMapper javaPropsMapper = new JavaPropsMapper();

    public ConfigurationBuilder() {
        this(new Configuration.Builder(), "xorcery");
    }

    public ConfigurationBuilder(String baseName) {
        this(new Configuration.Builder(), baseName);
    }

    public ConfigurationBuilder addYaml(InputStream yamlStream) throws UncheckedIOException {
        try (yamlStream) {
            JsonNode jsonNode = yamlMapper.readTree(yamlStream);
            if (jsonNode instanceof ObjectNode on)
            {
                new JsonMerger().merge(builder.builder(), on);
            }
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        return this;
    }

    public ConfigurationBuilder addYaml(String yamlString) throws UncheckedIOException {
        try {
            if (yamlMapper.readTree(yamlString) instanceof ObjectNode yaml)
            {
                new JsonMerger().merge(builder.builder(), yaml);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return this;
    }

    public ConfigurationBuilder addResource(String name) {
        Resources.getResource(name).ifPresent(resource ->
        {
            try (InputStream in = resource.openStream()) {
                addYaml(in);
                logger.log("Loaded " + resource);
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        });
        return this;
    }

    public ConfigurationBuilder addResources(String name) {
        for (URL resource : Resources.getResources(name)) {
            try (InputStream configurationStream = resource.openStream()) {
                addYaml(configurationStream);
                logger.log("Loaded " + resource);
            } catch (IOException ex) {
                throw new UncheckedIOException("Error loading resource configuration file:" + resource.toExternalForm(), ex);
            } catch (UncheckedIOException ex) {
                throw new UncheckedIOException("Error loading resource configuration file:" + resource.toExternalForm(), ex.getCause());
            }
        }
        return this;
    }

    public ConfigurationBuilder addFile(File configFile) {
        // Load specified overrides
        if (configFile != null) {
            try {
                if (configFile.getName().endsWith("yaml") || configFile.getName().endsWith("yml")) {
                    addYaml(new FileInputStream(configFile));
                    logger.log("Loaded " + configFile);
                } else if (configFile.getName().endsWith("properties")) {
                    addProperties(new FileInputStream(configFile));
                    logger.log("Loaded " + configFile);
                } else {
                    logger.log("Unknown configuration filetype: " + configFile);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return this;
    }

    public ConfigurationBuilder addDefaults()
    {
        addModules();
        addModuleOverrides();
        addApplication();

        addEtc();
        addHome();
        addUserDirectory();

        addConfigurationProviders();
        return this;
    }

    public ConfigurationBuilder addTestDefaults()
    {
        // First add the standard configuration
        addModules();
        addModuleOverrides();
        addApplication();

        // Then add the test configuration on top
        addModulesTest();
        addApplicationTest();

        addConfigurationProviders();
        return this;
    }

    public ConfigurationBuilder addConfigurationProviders() {

        ServiceLoader<ConfigurationProvider> load = ServiceLoader.load(ConfigurationProvider.class);
        for (ConfigurationProvider configurationProvider : load) {
            builder.add(configurationProvider.getNamespace(), new ConfigurationProviderContainerNode(configurationProvider));
            logger.log("Added "+configurationProvider.getNamespace()+" configuration provider");
        }

        return this;
    }

    public ConfigurationBuilder addModules() throws UncheckedIOException {
        // Load modules
        return addResources("META-INF/"+baseName+".yaml");
    }

    public ConfigurationBuilder addModuleOverrides() throws UncheckedIOException {
        // Load module overrides, to allow modules to change defaults from other modules
        return addResources("META-INF/"+baseName+"-override.yaml");
    }

    public ConfigurationBuilder addModulesTest() throws UncheckedIOException {
        // Load module overrides for tests
        return addResources("META-INF/"+baseName+"-test.yaml");
    }

    public ConfigurationBuilder addApplication() throws UncheckedIOException {
        // Remove any existing $schema reference, they would only be for partial schemas anyway
        builder.builder().remove("$schema");
        // Load application config file
        return addResources(baseName+".yaml");
    }

    public ConfigurationBuilder addApplicationTest() throws UncheckedIOException {
        // Load application test config file
        return addResources(baseName+"-test.yaml");
    }

    public ConfigurationBuilder addHome() throws UncheckedIOException {
        // Load user home overrides
        try {
            File userYamlFile = new File(System.getProperty("user.home"), ".xorcery/"+baseName+".yaml");
            if (userYamlFile.exists()) {
                FileInputStream userYamlStream = new FileInputStream(userYamlFile);
                addYaml(userYamlStream);
                logger.log("Loaded " + userYamlFile);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return this;
    }

    public ConfigurationBuilder addEtc() throws UncheckedIOException {
        // Load user home overrides
        try {
            String applicationName = builder.builder().path("application").path("name").asText();
            File etcApplicationYamlFile = new File("/etc/"+ applicationName, applicationName+".yaml");
            if (etcApplicationYamlFile.exists()) {
                FileInputStream etcApplicationYamlStream = new FileInputStream(etcApplicationYamlFile);
                addYaml(etcApplicationYamlStream);
                logger.log("Loaded " + etcApplicationYamlFile);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return this;
    }

    public ConfigurationBuilder addUserDirectory() throws UncheckedIOException {
        // Load user directory overrides
        try {
            File overridesYamlFile = new File(System.getProperty("user.dir"), baseName+".yaml");
            if (overridesYamlFile.exists()) {
                FileInputStream overridesYamlStream = new FileInputStream(overridesYamlFile);
                addYaml(overridesYamlStream);
                logger.log("Loaded " + overridesYamlFile);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return this;
    }

    public ConfigurationBuilder addProperties(InputStream propertiesStream) throws UncheckedIOException {
        try (propertiesStream) {
            if (javaPropsMapper.readTree(propertiesStream) instanceof ObjectNode properties)
            {
                new JsonMerger().merge(builder.builder(), properties);
            }
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        return this;
    }

    public ConfigurationBuilder addProperties(String propertiesString) throws UncheckedIOException {
        try {
            if (javaPropsMapper.readTree(propertiesString) instanceof ObjectNode properties)
            {
                new JsonMerger().merge(builder.builder(), properties);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    public ConfigurationBuilder with(Consumer<ConfigurationBuilder>... consumers)
    {
        for ( Consumer<ConfigurationBuilder> consumer : consumers )
        {
            consumer.accept( this );
        }
        return this;

    }

    public Configuration build()
    {
        return builder.build();
    }
}
