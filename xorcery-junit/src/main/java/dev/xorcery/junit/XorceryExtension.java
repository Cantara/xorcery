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
package dev.xorcery.junit;

import dev.xorcery.configuration.Configuration;
import dev.xorcery.configuration.ConfigurationLogger;
import dev.xorcery.configuration.InstanceConfiguration;
import dev.xorcery.configuration.builder.ConfigurationBuilder;
import dev.xorcery.core.Xorcery;
import dev.xorcery.io.ZipFiles;
import dev.xorcery.log4j.LoggerContextFactory;
import dev.xorcery.util.Resources;
import org.apache.logging.log4j.LogManager;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.ServiceLocatorFactory;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.*;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 * JUnit Extension to create Xorcery instances. It is possible to specify instance id (to easily look up the instance within tests), configuration,
 * and optionally provide a Zip archive name from src/test/resources which is unzipped into home directory data.
 * <p/>
 * If an archive is provided the home directory data is not deleted after the test finishes to make subsequent tests with the same test data run faster.
 * <p/>
 * If used with @Nested test classes, then only the root XorceryExtension will be initialized, and all nested classes will use the root instance. This makes
 * it easier to create large suites of tests running with the same Xorcery instance. This can be useful if it is relatively expensive to instantiate the Xorcery instance.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class XorceryExtension
        implements
        TestExecutionExceptionHandler,
        ParameterResolver,
        BeforeAllCallback,
        AfterAllCallback {
    public static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create("xorcery");

    private final List<Object> services;
    private final Configuration configuration;
    private final boolean isArchive;
    private Xorcery xorcery;
    private File tempDir;
    private boolean hasError = false;

    public static Builder xorcery() {
        return new Builder();
    }

    public static final class Builder {

        private String instanceId;
        private String archiveFileName;
        private String targetDir;
        private ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
        private List<Object> services = new ArrayList<>();

        public Builder id(String instanceId) {
            this.instanceId = instanceId;
            return this;
        }

        public Builder archive(String archiveFileName) {
            return archive(archiveFileName, archiveFileName.split("\\.")[0]);
        }

        public Builder archive(String archiveFileName, String targetDir) {
            this.archiveFileName = archiveFileName;
            this.targetDir = targetDir;
            return this;
        }

        public Builder configuration(Consumer<ConfigurationBuilder> configurationBuilderConsumer) {
            configurationBuilderConsumer.accept(configurationBuilder);
            return this;
        }

        public Builder addYaml(String yamlConfig) {
            configurationBuilder.addYaml(yamlConfig);
            return this;
        }


        public Builder with(Object service) {
            services.add(service);
            return this;
        }

        public XorceryExtension build() {

            if (instanceId != null)
                configurationBuilder.builder().add("instance.id", instanceId);

            try {
                File tempDir;
                if (archiveFileName == null) {
                    tempDir = Files.createTempDirectory(Path.of("target"), "xorcery").toFile();
                } else {
                    File zipFile = Path.of(Resources.getResource(archiveFileName).orElseThrow(() -> new IllegalArgumentException("File not found in classpath:" + archiveFileName)).toURI()).toFile();
                    tempDir = Path.of("target", targetDir).toFile();
                    if (!tempDir.exists()) {
                        ConfigurationLogger.getLogger().log("Unzipping " + archiveFileName);
                        ZipFiles.unzip(zipFile, tempDir);
                    } else if (tempDir.lastModified() < zipFile.lastModified()) {
                        ConfigurationLogger.getLogger().log("Updating " + archiveFileName);
                        Files.walk(tempDir.toPath())
                                .sorted(Comparator.reverseOrder())
                                .map(Path::toFile)
                                .forEach(File::delete);
                        ZipFiles.unzip(zipFile, tempDir);
                    } else {
                        ConfigurationLogger.getLogger().log("Unzipped " + archiveFileName + " already exists");
                    }
                }

                configurationBuilder.builder().add("instance.home", tempDir.getAbsolutePath());

                return new XorceryExtension(archiveFileName != null, tempDir, services, configurationBuilder.build());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }

        }
    }

    public XorceryExtension(boolean isArchive, File tempDir, List<Object> services, Configuration configuration) {
        this.isArchive = isArchive;
        this.tempDir = tempDir;
        this.services = services;
        this.configuration = configuration;
    }

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {

        if (!extensionContext.getEnclosingTestClasses().isEmpty())
            return;

        try {
            LoggerContextFactory.initialize(configuration);

            // Log final configuration
            LogManager.getLogger(getClass()).debug("Configuration:{}\n", configuration);

            ServiceLocator serviceLocator = null;
            if (!services.isEmpty()) {
                serviceLocator = ServiceLocatorFactory.getInstance().create(null);
                for (Object service : services) {
                    serviceLocator.inject(service);
                    ServiceLocatorUtilities.addOneConstant(serviceLocator, service);
                }
            }
            xorcery = new Xorcery(configuration, serviceLocator);
        } finally {
            List<String> messages = ConfigurationLogger.getLogger().drain();
            for (String message : messages) {
                System.out.println(message);
            }
        }

        ExtensionContext.Store store = extensionContext.getRoot().getStore(NAMESPACE);
        store.put(InstanceConfiguration.get(configuration).getId(), xorcery);
        store.put(ExtensionContext.class, extensionContext);
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) throws Exception {
        if (!extensionContext.getEnclosingTestClasses().isEmpty())
            return;

        if (xorcery != null) {
            xorcery.close();

            // Allow archive temp dirs to survive between tests to speed things up
            if (!isArchive && !hasError) {
                Files.walk(tempDir.toPath())
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        }
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        Class<?> type = parameterContext.getParameter().getType();
        return xorcery.getServiceLocator().getService(type) != null ||
                type.getConstructors().length > 0;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        Class<?> type = parameterContext.getParameter().getType();
        Object result = xorcery.getServiceLocator().getService(type);
        if (result == null)
            result = xorcery.getServiceLocator().createAndInitialize(type);
        return result;
    }

    @Override
    public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        hasError = true;
        throw throwable;
    }

    public Xorcery getXorcery() {
        return xorcery;
    }

    public ServiceLocator getServiceLocator() {
        return xorcery.getServiceLocator();
    }

    public Configuration getConfiguration() {
        return configuration;
    }
}
