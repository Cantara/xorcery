package com.exoreaction.xorcery.configuration;

import com.exoreaction.xorcery.json.JsonElement;
import com.exoreaction.xorcery.json.JsonMerger;
import com.exoreaction.xorcery.json.VariableResolver;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

/**
 * @author rickardoberg
 * @since 20/04/2022
 */
public record Configuration(ObjectNode json)
        implements JsonElement {
    private static final Logger logger = LogManager.getLogger(Configuration.class);

    public record Builder(ObjectNode builder) {

        public static Builder load(File configFile)
                throws IOException {
            Configuration.Builder builder = new Configuration.Builder();

            // Load system properties and environment variables
            builder.addSystemProperties("SYSTEM");
            builder.addEnvironmentVariables("ENV");

            // Load defaults
            {
                URL resource = Configuration.class.getResource("/xorcery-defaults.yaml");
                try (InputStream in = resource.openStream()) {
                    builder = builder.addYaml(in);
                    logger.info("Loaded " + resource);
                }
            }

            // Load custom
            Enumeration<URL> configurationURLs = Configuration.class.getClassLoader().getResources("xorcery.yaml");
            while (configurationURLs.hasMoreElements()) {
                URL resource = configurationURLs.nextElement();
                try (InputStream configurationStream = resource.openStream()) {
                    builder = builder.addYaml(configurationStream);
                    logger.info("Loaded " + resource);
                }
            }

            // Load user directory overrides
            File overridesYamlFile = new File(System.getProperty("user.dir"), "xorcery.yaml");
            if (overridesYamlFile.exists()) {
                FileInputStream overridesYamlStream = new FileInputStream(overridesYamlFile);
                builder = builder.addYaml(overridesYamlStream);
                logger.info("Loaded " + overridesYamlFile);
            }

            // Load specified overrides
            if (configFile != null) {

                if (configFile.getName().endsWith("yaml") || configFile.getName().endsWith("yml")) {
                    builder = builder.addYaml(new FileInputStream(configFile));
                    logger.info("Loaded " + configFile);
                } else if (configFile.getName().endsWith("properties")) {
                    builder = builder.addProperties(new FileInputStream(configFile));
                    logger.info("Loaded " + configFile);
                } else
                {
                    logger.warn("Unknown configuration filetype: " + configFile);
                }
            }

            // Load user home overrides
            File userYamlFile = new File(System.getProperty("user.home"), "xorcery/xorcery.yaml");
            if (userYamlFile.exists()) {
                FileInputStream userYamlStream = new FileInputStream(userYamlFile);
                builder = builder.addYaml(userYamlStream);
                logger.info("Loaded " + userYamlFile);
            }

            return builder;
        }

        public static Builder loadTest(File configFile)
                throws IOException {
            Configuration.Builder builder = load(configFile);

            // Load defaults
            {
                URL resource = Configuration.class.getResource("/xorcery-defaults-test.yaml");
                try (InputStream in = resource.openStream()) {
                    builder = builder.addYaml(in);
                    logger.info("Loaded " + resource);
                }
            }

            // Load custom
            Enumeration<URL> configurationURLs = Configuration.class.getClassLoader().getResources("xorcery-test.yaml");
            while (configurationURLs.hasMoreElements()) {
                URL resource = configurationURLs.nextElement();
                try (InputStream configurationStream = resource.openStream()) {
                    builder = builder.addYaml(configurationStream);
                    logger.info("Loaded " + resource);
                }
            }

            // Load user directory overrides
            Configuration partialConfig = builder.build();
            StandardConfiguration standardConfiguration = new StandardConfiguration.Impl(partialConfig);
            builder = partialConfig.asBuilder();
            File overridesYamlFile = new File(standardConfiguration.getHome(), "xorcery-test.yaml");
            if (overridesYamlFile.exists()) {
                FileInputStream overridesYamlStream = new FileInputStream(overridesYamlFile);
                builder = builder.addYaml(overridesYamlStream);
                logger.info("Loaded " + overridesYamlFile);
            }

            // Load specified overrides
            if (configFile != null) {

                if (configFile.getName().endsWith("yaml") || configFile.getName().endsWith("yml")) {
                    builder = builder.addYaml(new FileInputStream(configFile));
                    logger.info("Loaded " + configFile);
                } else if (configFile.getName().endsWith("properties")) {
                    builder = builder.addProperties(new FileInputStream(configFile));
                    logger.info("Loaded " + configFile);
                } else
                {
                    logger.warn("Unknown configuration filetype: " + configFile);
                }
            }

            // Load user overrides
            File userYamlFile = new File(System.getProperty("user.home"), "xorcery/xorcery-test.yaml");
            if (userYamlFile.exists()) {
                FileInputStream userYamlStream = new FileInputStream(userYamlFile);
                builder = builder.addYaml(userYamlStream);
                logger.info("Loaded " + userYamlFile);
            }

            return builder;
        }

        public Builder() {
            this(JsonNodeFactory.instance.objectNode());
        }

        public Builder add(String name, JsonNode value) {
            builder.set(name, value);
            return this;
        }

        public Builder add(String name, String value) {
            return add(name, builder.textNode(value));
        }

        public Builder add(String name, long value) {
            return add(name, builder.numberNode(value));
        }

        public Builder addYaml(InputStream yamlStream) throws IOException {
            try (yamlStream) {
                ObjectNode yaml = (ObjectNode) new ObjectMapper(new YAMLFactory()).readTree(yamlStream);
                new JsonMerger().merge(builder, yaml);
                return this;
            }
        }

        public Builder addYaml(String yamlString) throws IOException {
            ObjectNode yaml = (ObjectNode) new ObjectMapper(new YAMLFactory()).readTree(yamlString);
            new JsonMerger().merge(builder, yaml);
            return this;
        }

        private Builder addProperties(InputStream propertiesStream) throws IOException {
            try (propertiesStream) {
                ObjectNode properties = (ObjectNode) new ObjectMapper(new JavaPropsFactory()).readTree(propertiesStream);
                new JsonMerger().merge(builder, properties);
                return this;
            }
        }

        public Builder addProperties(String propertiesString) throws IOException {
            ObjectNode properties = (ObjectNode) new ObjectMapper(new JavaPropsFactory()).readTree(propertiesString);
            new JsonMerger().merge(builder, properties);
            return this;
        }

        public Builder addSystemProperties(String nodeName) {
            ObjectNode system = builder.objectNode();
            for (Map.Entry<Object, Object> systemProperty : System.getProperties().entrySet()) {
                system.set(systemProperty.getKey().toString().replace('.', '_'), builder.textNode(systemProperty.getValue().toString()));
            }
            builder.set(nodeName, system);
            return this;
        }

        public Builder addEnvironmentVariables(String nodeName) {
            ObjectNode env = builder.objectNode();
            System.getenv().forEach((key, value) -> env.set(key.replace('.', '_'), env.textNode(value)));
            builder.set(nodeName, env);
            return this;
        }

        public Configuration build() {
            // Resolve any references
            return new Configuration(new VariableResolver().apply(builder, builder));
        }
    }

    public Configuration getConfiguration(String name) {
        return getJson(name)
                .map(ObjectNode.class::cast).map(Configuration::new)
                .orElseGet(() -> new Configuration(JsonNodeFactory.instance.objectNode()));
    }

    public List<Configuration> getConfigurations(String name) {
        return getJson(name)
                .map(ArrayNode.class::cast)
                .map(a -> JsonElement.getValuesAs(a, Configuration::new))
                .orElseGet(Collections::emptyList);
    }

    public Builder asBuilder() {
        return new Builder(json);
    }
}

