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
package dev.xorcery.core;

import dev.xorcery.configuration.Configuration;
import dev.xorcery.configuration.ConfigurationLogger;
import dev.xorcery.configuration.InstanceConfiguration;
import dev.xorcery.core.hk2.ConfigurationPostPopulatorProcessor;
import dev.xorcery.core.hk2.Hk2Configuration;
import dev.xorcery.core.hk2.UniqueDescriptorFileFinder;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.spi.LoggerContext;
import org.glassfish.hk2.api.*;
import org.glassfish.hk2.extras.events.internal.DefaultTopicDistributionService;
import org.glassfish.hk2.runlevel.RunLevelController;
import org.glassfish.hk2.runlevel.RunLevelFuture;
import org.glassfish.hk2.utilities.BuilderHelper;
import org.glassfish.hk2.utilities.ClasspathDescriptorFileFinder;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.TimeZone;
import java.util.concurrent.CompletableFuture;

/**
 * Xorcery run level values:
 * 0: Configuration refresh
 * 2: Certificate request/refresh
 * 4: Servers and Clients
 * 6: Server publishers/subscribers
 * 8: Client publishers/subscribers
 * 18: Server start/stop
 * 20: DNS registration/deregistration
 *
 * @author rickardoberg
 * @since 12/04/2022
 */
public final class Xorcery
        implements AutoCloseable {

    private final ServiceLocator serviceLocator;

    private final Logger logger;
    private final Marker marker;
    private final CompletableFuture<Void> closed = new CompletableFuture<>();

    public Xorcery(Configuration configuration) throws Exception {
        this(configuration, null);
    }

    public Xorcery(Configuration configuration, ServiceLocator sl) throws Exception {

        // Set configured system properties
        Configuration system = configuration.getConfiguration("system");
        system.json().fields().forEachRemaining(entry ->
        {
            if (!entry.getValue().isNull()) {
                String key = entry.getKey();
                String value = entry.getValue().asText();
                ConfigurationLogger.getLogger().log("Set system property '" + key + "' to '" + value + "'");
                System.setProperty(entry.getKey(), entry.getValue().asText());
            }
        });

        InstanceConfiguration instanceConfiguration = InstanceConfiguration.get(configuration);
        // Set locale
        instanceConfiguration.getLocale().ifPresent(Locale::setDefault);
        // Set time zone
        instanceConfiguration.getTimeZone().ifPresent(TimeZone::setDefault);

        // Ensure home directory exists
        boolean createdHome = false;
        File homeDir = new File(instanceConfiguration.getHome());
        if (!homeDir.exists())
            createdHome = homeDir.mkdirs();

        Hk2Configuration hk2Configuration = new Hk2Configuration(configuration.getConfiguration("hk2"));

        this.serviceLocator = sl == null ? ServiceLocatorFactory.getInstance().create(null) : sl;

        Filter configurationFilter = getEnabledServicesFilter(configuration);
        Logger xorceryLogger = null;
        Marker xorceryMarker = null;
        try {
            PopulatorPostProcessor populatorPostProcessor = new ConfigurationPostPopulatorProcessor(configuration, ConfigurationLogger.getLogger()::log);
            populateServiceLocator(serviceLocator, configuration, hk2Configuration, populatorPostProcessor);
            setupServiceLocator(serviceLocator, hk2Configuration);

            // Instantiate all enabled services
            LoggerContext loggerContext = serviceLocator.getService(LoggerContext.class);
            xorceryLogger = loggerContext.getLogger(Xorcery.class);
            xorceryMarker = MarkerManager.getMarker(instanceConfiguration.getId());
            this.logger = xorceryLogger;
            this.marker = xorceryMarker;
            List<String> messages = ConfigurationLogger.getLogger().drain();
            if (xorceryLogger.isDebugEnabled()) {
                for (String msg : messages) {
                    xorceryLogger.debug(msg);
                }
            }

            if (createdHome)
                xorceryLogger.info(xorceryMarker, "Create home directory " + homeDir);
            xorceryLogger.info(xorceryMarker, "Starting");

            RunLevelController runLevelController = serviceLocator.getService(RunLevelController.class);
            runLevelController.setThreadingPolicy(hk2Configuration.getThreadingPolicy());
            runLevelController.setMaximumUseableThreads(hk2Configuration.getMaximumUseableThreads());

            runLevelController.proceedTo(hk2Configuration.getRunLevel());

            if (xorceryLogger.isDebugEnabled()) {
                StringBuilder msg = new StringBuilder();
                List<ServiceHandle<?>> services = serviceLocator.getAllServiceHandles(configurationFilter);
                for (ServiceHandle<?> service : services) {
                    msg.append('\n').append(service.getActiveDescriptor().getImplementation());
                }
                xorceryLogger.debug(xorceryMarker, "Services:" + msg);
            }

            xorceryLogger.info(xorceryMarker, "Started");
        } catch (MultiException e) {
            if (xorceryLogger != null && !xorceryLogger.isDebugEnabled()) {
                List<ServiceHandle<?>> services = serviceLocator.getAllServiceHandles(configurationFilter);
                StringBuilder msg = new StringBuilder();
                for (ServiceHandle<?> service : services) {
                    msg.append('\n').append(service.getActiveDescriptor().getImplementation());
                }

                xorceryLogger.error(xorceryMarker, "Startup failed. Configuration:\n{}\nServices:{}", configuration, msg);
            }
            throw e;
        }
    }

    public ServiceLocator getServiceLocator() {
        return serviceLocator;
    }

    public void close(Throwable throwable) {
        synchronized (this) {
            if (!closed.isDone()) {
                if (logger != null)
                    logger.info(marker, "Stopping");
                RunLevelController runLevelController = serviceLocator.getService(RunLevelController.class);
                RunLevelFuture currentProceeding = runLevelController.getCurrentProceeding();
                if (currentProceeding != null)
                {
                    // Wait for current to finish
                    try {
                        currentProceeding.get();
                    } catch (Throwable t) {
                        // Ignore
                    }
                }
                runLevelController.proceedTo(-1);
                serviceLocator.shutdown();
                if (logger != null)
                    logger.info(marker, "Stopped");

                // Notify anyone waiting for this instance to close
                if (throwable == null)
                    closed.complete(null);
                else
                    closed.completeExceptionally(throwable);
                this.notifyAll();
            }
        }
    }

    public void close() {
        close(null);
    }

    public CompletableFuture<Void> getClosed() {
        return closed;
    }

    private Filter getEnabledServicesFilter(Configuration configuration) {
        return d ->
        {
            boolean result = Optional.ofNullable(d.getName())
                    .map(name -> configuration.getBoolean(name + ".enabled")
                            .orElseGet(() -> configuration.getBoolean("defaults.enabled").orElse(false)))
                    .orElse(true);
            return result;
        };
    }

    private void populateServiceLocator(ServiceLocator serviceLocator, Configuration configuration, Hk2Configuration hk2Configuration, PopulatorPostProcessor populatorPostProcessor) throws MultiException {
        DynamicConfigurationService dcs = serviceLocator.getService(DynamicConfigurationService.class);

        DynamicConfiguration dynamicConfiguration = ServiceLocatorUtilities.createDynamicConfiguration(serviceLocator);
        dynamicConfiguration.addActiveDescriptor(BuilderHelper.createConstantDescriptor(configuration));
        dynamicConfiguration.addActiveDescriptor(BuilderHelper.createConstantDescriptor(this));
        dynamicConfiguration.addActiveDescriptor(DefaultTopicDistributionService.class);
        dynamicConfiguration.commit();

        Populator populator = dcs.getPopulator();

        try {
            populator.populate(new UniqueDescriptorFileFinder(new ClasspathDescriptorFileFinder(Xorcery.class.getClassLoader(), hk2Configuration.getDescriptorNames())),
                    populatorPostProcessor);
        } catch (IOException e) {
            throw new MultiException(e);
        }
    }

    private void setupServiceLocator(ServiceLocator serviceLocator, Hk2Configuration configuration) {
        if (configuration.isImmediateScopeEnabled()) {
            ImmediateController immediateController = ServiceLocatorUtilities.enableImmediateScopeSuspended(serviceLocator);
            immediateController.setImmediateState(configuration.getImmediateScopeState());
        }
        if (configuration.isThreadScopeEnabled()) {
            ServiceLocatorUtilities.enablePerThreadScope(serviceLocator);
        }
    }

}
