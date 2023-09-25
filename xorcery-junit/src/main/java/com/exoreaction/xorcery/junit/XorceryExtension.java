package com.exoreaction.xorcery.junit;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.configuration.InstanceConfiguration;
import com.exoreaction.xorcery.configuration.builder.ConfigurationBuilder;
import com.exoreaction.xorcery.configuration.builder.ConfigurationLogger;
import com.exoreaction.xorcery.core.LoggerContextFactory;
import com.exoreaction.xorcery.core.Xorcery;
import com.exoreaction.xorcery.io.ZipFiles;
import com.exoreaction.xorcery.util.Resources;
import org.glassfish.hk2.api.ServiceLocator;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.*;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.function.Consumer;

/**
 * JUnit Extension to create Xorcery instances. It is possible to specify instance id (to easily look up the instance within tests), configuration,
 * and optionally provide a Zip archive name from src/test/resources which is unzipped into home directory data.
 * <p>
 * If an archive is provided the home directory data is not deleted after the test finishes to make subsequent tests with the same test data run faster.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class XorceryExtension
        implements
        TestExecutionExceptionHandler,
        ParameterResolver,
        BeforeAllCallback,
        AfterAllCallback {
    public static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create("xorcery");

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

        public XorceryExtension build() {

            if (instanceId != null)
                configurationBuilder.with(b -> b.add("instance.id", instanceId));

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

                configurationBuilder.with(b -> b.add("instance.home", tempDir.getAbsolutePath()));

                return new XorceryExtension(archiveFileName != null, tempDir, configurationBuilder.build());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }

        }
    }

    public XorceryExtension(boolean isArchive, File tempDir, Configuration configuration) {
        this.isArchive = isArchive;
        this.tempDir = tempDir;
        this.configuration = configuration;
    }

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {

        try {
            // Log final configuration
            ConfigurationLogger.getLogger().log("Configuration:\n" + configuration);
            LoggerContextFactory.initialize(configuration);
            xorcery = new Xorcery(configuration);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        ExtensionContext.Store store = extensionContext.getRoot().getStore(NAMESPACE);
        store.put(InstanceConfiguration.get(configuration).getId(), xorcery);
        store.put(ExtensionContext.class, extensionContext);
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
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
        return extensionContext.getStore(NAMESPACE).get(parameterContext.getParameter().getType()) != null;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return extensionContext.getStore(NAMESPACE).get(parameterContext.getParameter().getType());
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
        return xorcery.getServiceLocator().getService(Configuration.class);
    }
}
