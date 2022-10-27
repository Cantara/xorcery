package com.exoreaction.xorcery.configuration.builder;

import com.exoreaction.xorcery.configuration.model.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.URL;
import java.util.Enumeration;
import java.util.function.Consumer;

public class StandardConfigurationBuilder {

    private static final Logger logger = LogManager.getLogger(Configuration.class);

    public StandardConfigurationBuilder() {
    }

    public void addDefaults(Configuration.Builder builder)
            throws UncheckedIOException {

        addXorceryDefaults(builder);
        addExtensions(builder);
        addApplication(builder);

        addUserDirectory(builder);
        addHome(builder);

        addSystemProperties(builder);
        addEnvironmentVariables(builder);
    }


    public void addTestDefaults(Configuration.Builder builder)
            throws UncheckedIOException {

        addXorceryDefaults(builder);
        addXorceryTestDefaults(builder);
        addExtensions(builder);
        addExtensionsTest(builder);
        addApplication(builder);
        addApplicationTest(builder);

        addUserDirectory(builder);

        addSystemProperties(builder);
        addEnvironmentVariables(builder);
    }

    public Consumer<Configuration.Builder> addTestDefaultsWithYaml(String yamlString)
            throws UncheckedIOException {
        return builder ->
        {
            addXorceryDefaults(builder);
            addXorceryTestDefaults(builder);
            addExtensions(builder);
            addExtensionsTest(builder);
            addApplication(builder);
            addApplicationTest(builder);

            addUserDirectory(builder);

            addSystemProperties(builder);
            addEnvironmentVariables(builder);

            new YamlConfigurationBuilder(builder).addYaml(yamlString);
        };
    }


    public void addSystemProperties(Configuration.Builder builder) {
        builder.addSystemProperties("SYSTEM");
    }

    public void addEnvironmentVariables(Configuration.Builder builder) {
        builder.addEnvironmentVariables("ENV");
    }

    public void addXorceryDefaults(Configuration.Builder builder) throws UncheckedIOException {
        // Load Xorcery defaults
        URL resource = Configuration.class.getClassLoader().getResource("META-INF/xorcery-defaults.yaml");
        if (resource == null)
            throw new UncheckedIOException(new IOException("Resource not found: META-INF/xorcery-defaults.yaml"));
        try (InputStream in = resource.openStream()) {
            new YamlConfigurationBuilder(builder).addYaml(in);
            logger.info("Loaded " + resource);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    public void addExtensions(Configuration.Builder builder) throws UncheckedIOException {
        // Load extensions
        try {
            Enumeration<URL> extensionConfigurationURLs = ClassLoader.getSystemResources("META-INF/xorcery.yaml");
            while (extensionConfigurationURLs.hasMoreElements()) {
                URL resource = extensionConfigurationURLs.nextElement();
                try (InputStream configurationStream = resource.openStream()) {
                    new YamlConfigurationBuilder(builder).addYaml(configurationStream);
                    logger.info("Loaded " + resource);
                } catch (IOException ex) {
                    throw new UncheckedIOException("Error loading configuration file:" + resource.toExternalForm(), ex);
                } catch (UncheckedIOException ex) {
                    throw new UncheckedIOException("Error loading configuration file:" + resource.toExternalForm(), ex.getCause());
                }
            }
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    public void addExtensionsTest(Configuration.Builder builder) throws UncheckedIOException {
        // Load extensions
        try {
            Enumeration<URL> extensionConfigurationURLs = ClassLoader.getSystemResources("META-INF/xorcery-test.yaml");
            while (extensionConfigurationURLs.hasMoreElements()) {
                URL resource = extensionConfigurationURLs.nextElement();
                try (InputStream configurationStream = resource.openStream()) {
                    new YamlConfigurationBuilder(builder).addYaml(configurationStream);
                    logger.info("Loaded " + resource);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void addApplication(Configuration.Builder builder) throws UncheckedIOException {
        // Load application config file
        try {
            Enumeration<URL> configurationURLs = ClassLoader.getSystemResources("xorcery.yaml");
            while (configurationURLs.hasMoreElements()) {
                URL resource = configurationURLs.nextElement();
                try (InputStream configurationStream = resource.openStream()) {
                    new YamlConfigurationBuilder(builder).addYaml(configurationStream);
                    logger.info("Loaded " + resource);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void addApplicationTest(Configuration.Builder builder) throws UncheckedIOException {
        // Load application config file
        try {
            Enumeration<URL> configurationURLs = ClassLoader.getSystemResources("xorcery-test.yaml");
            while (configurationURLs.hasMoreElements()) {
                URL resource = configurationURLs.nextElement();
                try (InputStream configurationStream = resource.openStream()) {
                    new YamlConfigurationBuilder(builder).addYaml(configurationStream);
                    logger.info("Loaded " + resource);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void addUserDirectory(Configuration.Builder builder) throws UncheckedIOException {
        // Load user directory overrides
        try {
            File overridesYamlFile = new File(System.getProperty("user.dir"), "xorcery.yaml");
            if (overridesYamlFile.exists()) {
                FileInputStream overridesYamlStream = new FileInputStream(overridesYamlFile);
                new YamlConfigurationBuilder(builder).addYaml(overridesYamlStream);
                logger.info("Loaded " + overridesYamlFile);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void addHome(Configuration.Builder builder) throws UncheckedIOException {
        // Load user home overrides
        try {
            File userYamlFile = new File(System.getProperty("user.home"), "xorcery/xorcery.yaml");
            if (userYamlFile.exists()) {
                FileInputStream userYamlStream = new FileInputStream(userYamlFile);
                new YamlConfigurationBuilder(builder).addYaml(userYamlStream);
                logger.info("Loaded " + userYamlFile);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public Consumer<Configuration.Builder> addFile(File configFile) throws UncheckedIOException {
        return builder ->
        {
            // Load specified overrides
            if (configFile != null) {
                try {
                    if (configFile.getName().endsWith("yaml") || configFile.getName().endsWith("yml")) {
                        new YamlConfigurationBuilder(builder).addYaml(new FileInputStream(configFile));
                        logger.info("Loaded " + configFile);
                    } else if (configFile.getName().endsWith("properties")) {
                        new PropertiesConfigurationBuilder(builder).addProperties(new FileInputStream(configFile));
                        logger.info("Loaded " + configFile);
                    } else {
                        logger.warn("Unknown configuration filetype: " + configFile);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    public void addXorceryTestDefaults(Configuration.Builder builder) throws UncheckedIOException {
        // Load Xorcery defaults
        try {
            URL resource = ClassLoader.getSystemResource("META-INF/xorcery-defaults-test.yaml");
            try (InputStream in = resource.openStream()) {
                new YamlConfigurationBuilder(builder).addYaml(in);
                logger.info("Loaded " + resource);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
