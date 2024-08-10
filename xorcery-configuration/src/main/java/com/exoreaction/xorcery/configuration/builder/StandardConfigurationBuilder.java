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
package com.exoreaction.xorcery.configuration.builder;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.configuration.spi.ConfigurationProvider;
import com.exoreaction.xorcery.json.JsonMerger;
import com.exoreaction.xorcery.util.Resources;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import java.io.*;
import java.net.URL;
import java.util.ServiceLoader;
import java.util.function.Consumer;

public class StandardConfigurationBuilder {

    private static final ConfigurationLogger logger = ConfigurationLogger.getLogger();

    private static final YAMLMapper yamlMapper = new YAMLMapper();
    private static final JavaPropsMapper javaPropsMapper = new JavaPropsMapper();
    
    private String baseName;
    
    public StandardConfigurationBuilder(String baseName) {
        this.baseName = baseName;
    }

    public StandardConfigurationBuilder() {
        this("xorcery");
    }

    public void addDefaults(Configuration.Builder builder)
            throws UncheckedIOException {

        addDefault(builder);
        addModules(builder);
        addModuleOverrides(builder);
        addApplication(builder);

        addHome(builder);
        addUserDirectory(builder);

        addConfigurationProviders(builder);
    }


    public void addTestDefaults(Configuration.Builder builder)
            throws UncheckedIOException {

        // First add the standard configuration
        addDefault(builder);
        addModules(builder);
        addModuleOverrides(builder);
        addApplication(builder);

        // Then add the test configuration on top
        addXorceryTestDefaults(builder);
        addModulesTest(builder);
        addApplicationTest(builder);

        addConfigurationProviders(builder);
    }

    public Consumer<Configuration.Builder> addTestDefaultsWithYaml(String yamlString)
            throws UncheckedIOException {
        return builder ->
        {
            addTestDefaults(builder);

            addYaml(yamlString).accept(builder);
        };
    }

    public void addConfigurationProviders(Configuration.Builder builder) {

        ServiceLoader<ConfigurationProvider> load = ServiceLoader.load(ConfigurationProvider.class);
        for (ConfigurationProvider configurationProvider : load) {
            builder.add(configurationProvider.getNamespace(), new ConfigurationProviderContainerNode(configurationProvider));
            logger.log("Added "+configurationProvider.getNamespace()+" configuration provider");
        }
    }

    public void addDefault(Configuration.Builder builder) throws UncheckedIOException {
        // Load default
        Resources.getResource("META-INF/"+baseName+"-defaults.yaml").ifPresent(resource ->
                {
                    try (InputStream in = resource.openStream()) {
                        addYaml(in).accept(builder);
                        logger.log("Loaded " + resource);
                    } catch (IOException ex) {
                        throw new UncheckedIOException(ex);
                    }
                });
    }

    public void addModules(Configuration.Builder builder) throws UncheckedIOException {
        // Load modules
        for (URL resource : Resources.getResources("META-INF/"+baseName+".yaml")) {
            try (InputStream configurationStream = resource.openStream()) {
                addYaml(configurationStream).accept(builder);
                logger.log("Loaded " + resource);
            } catch (IOException ex) {
                throw new UncheckedIOException("Error loading module configuration file:" + resource.toExternalForm(), ex);
            } catch (UncheckedIOException ex) {
                throw new UncheckedIOException("Error loading module configuration file:" + resource.toExternalForm(), ex.getCause());
            }
        }
    }

    public void addModuleOverrides(Configuration.Builder builder) throws UncheckedIOException {
        // Load module overrides, to allow modules to change defaults from other modules
        for (URL resource : Resources.getResources("META-INF/"+baseName+"-override.yaml")) {
            try (InputStream configurationStream = resource.openStream()) {
                addYaml(configurationStream).accept(builder);
                logger.log("Loaded " + resource);
            } catch (IOException ex) {
                throw new UncheckedIOException("Error loading module override configuration file:" + resource.toExternalForm(), ex);
            } catch (UncheckedIOException ex) {
                throw new UncheckedIOException("Error loading module override configuration file:" + resource.toExternalForm(), ex.getCause());
            }
        }
    }

    public void addModulesTest(Configuration.Builder builder) throws UncheckedIOException {
        // Load extensions
        try {
            for (URL resource : Resources.getResources("META-INF/"+baseName+"-test.yaml")) {
                try (InputStream configurationStream = resource.openStream()) {
                    addYaml(configurationStream).accept(builder);
                    logger.log("Loaded " + resource);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void addApplication(Configuration.Builder builder) throws UncheckedIOException {
        // Load application config file
        try {
            for (URL resource : Resources.getResources(baseName+".yaml")) {
                try (InputStream configurationStream = resource.openStream()) {
                    addYaml(configurationStream).accept(builder);
                    logger.log("Loaded " + resource);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void addApplicationTest(Configuration.Builder builder) throws UncheckedIOException {
        // Load application config file
        try {
            for (URL resource : Resources.getResources(baseName+"-test.yaml")) {
                try (InputStream configurationStream = resource.openStream()) {
                    addYaml(configurationStream).accept(builder);
                    logger.log("Loaded " + resource);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void addHome(Configuration.Builder builder) throws UncheckedIOException {
        // Load user home overrides
        try {
            File userYamlFile = new File(System.getProperty("user.home"), ".xorcery/"+baseName+".yaml");
            if (userYamlFile.exists()) {
                FileInputStream userYamlStream = new FileInputStream(userYamlFile);
                addYaml(userYamlStream).accept(builder);
                logger.log("Loaded " + userYamlFile);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void addUserDirectory(Configuration.Builder builder) throws UncheckedIOException {
        // Load user directory overrides
        try {
            File overridesYamlFile = new File(System.getProperty("user.dir"), baseName+".yaml");
            if (overridesYamlFile.exists()) {
                FileInputStream overridesYamlStream = new FileInputStream(overridesYamlFile);
                addYaml(overridesYamlStream).accept(builder);
                logger.log("Loaded " + overridesYamlFile);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public Consumer<Configuration.Builder> addFile(File configFile) {
        return builder ->
        {
            // Load specified overrides
            if (configFile != null) {
                try {
                    if (configFile.getName().endsWith("yaml") || configFile.getName().endsWith("yml")) {
                        addYaml(new FileInputStream(configFile)).accept(builder);
                        logger.log("Loaded " + configFile);
                    } else if (configFile.getName().endsWith("properties")) {
                        addProperties(new FileInputStream(configFile)).accept(builder);
                        logger.log("Loaded " + configFile);
                    } else {
                        logger.log("Unknown configuration filetype: " + configFile);
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        };
    }

    public void addXorceryTestDefaults(Configuration.Builder builder) throws UncheckedIOException {
        // Load test defaults
        Resources.getResource("META-INF/"+baseName+"-defaults-test.yaml").ifPresent(resource ->
        {
            try (InputStream in = resource.openStream()) {
                addYaml(in).accept(builder);
                logger.log("Loaded " + resource);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    public Consumer<Configuration.Builder> addYaml(InputStream yamlStream) throws UncheckedIOException {
        return builder ->
        {
            try (yamlStream) {
                JsonNode jsonNode = yamlMapper.readTree(yamlStream);
                if (jsonNode instanceof ObjectNode on)
                {
                    new JsonMerger().merge(builder.builder(), on);
                }
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        };
    }

    public Consumer<Configuration.Builder> addYaml(String yamlString) throws UncheckedIOException {
        return builder ->
        {
            try {
                if (yamlMapper.readTree(yamlString) instanceof ObjectNode yaml)
                {
                    new JsonMerger().merge(builder.builder(), yaml);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };
    }

    public Consumer<Configuration.Builder> addProperties(InputStream propertiesStream) throws UncheckedIOException {
        return builder ->
        {
            try (propertiesStream) {
                if (javaPropsMapper.readTree(propertiesStream) instanceof ObjectNode properties)
                {
                    new JsonMerger().merge(builder.builder(), properties);
                }
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        };
    }

    public Consumer<Configuration.Builder> addProperties(String propertiesString) throws UncheckedIOException {
        return builder ->
        {
            try {
                if (javaPropsMapper.readTree(propertiesString) instanceof ObjectNode properties)
                {
                    new JsonMerger().merge(builder.builder(), properties);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };
    }
}
