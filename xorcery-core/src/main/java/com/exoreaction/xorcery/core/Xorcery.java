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
package com.exoreaction.xorcery.core;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.configuration.InstanceConfiguration;
import com.exoreaction.xorcery.configuration.builder.ConfigurationLogger;
import jakarta.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.spi.LoggerContext;
import org.glassfish.hk2.api.*;
import org.glassfish.hk2.extras.events.internal.DefaultTopicDistributionService;
import org.glassfish.hk2.runlevel.RunLevelController;
import org.glassfish.hk2.utilities.BuilderHelper;
import org.glassfish.hk2.utilities.ClasspathDescriptorFileFinder;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Xorcery run level values:
 * 0: Configuration refresh
 * 2: Certificate request/refresh
 * 4: Server
 * 6: Server publishers/subscribers
 * 8: Client publishers/subscribers
 * 18: Server start/stop
 * 20: DNS registration/deregistration
 *
 * @author rickardoberg
 * @since 12/04/2022
 */
public class Xorcery
        implements AutoCloseable {

    @Inject
    private Logger logger;
    @Inject
    private LoggerContext loggerContext;
    private final Marker marker;

    private final ServiceLocator serviceLocator;

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

        // Ensure home directory exists
        boolean createdHome = false;
        File homeDir = new File(InstanceConfiguration.get(configuration).getHome());
        if (!homeDir.exists())
            createdHome = homeDir.mkdirs();

        Hk2Configuration hk2Configuration = new Hk2Configuration(configuration.getConfiguration("hk2"));

        this.serviceLocator = sl == null ? ServiceLocatorFactory.getInstance().create(null) : sl;

        List<String> configurationMonitor = new ArrayList<>();
        PopulatorPostProcessor populatorPostProcessor = new ConfigurationPostPopulatorProcessor(configuration, configurationMonitor::add);
        populateServiceLocator(serviceLocator, configuration, hk2Configuration, populatorPostProcessor);
        setupServiceLocator(serviceLocator, hk2Configuration);

        // Instantiate all enabled services
        serviceLocator.inject(this);
        InstanceConfiguration instanceConfiguration = InstanceConfiguration.get(configuration);
        marker = MarkerManager.getMarker(instanceConfiguration.getId());
        Logger xorceryLogger = loggerContext.getLogger(Xorcery.class);
        if (xorceryLogger.isDebugEnabled()) {
            for (String msg : ConfigurationLogger.getLogger().drain()) {
                xorceryLogger.debug(msg);
            }
            for (String msg : configurationMonitor) {
                xorceryLogger.debug(msg);
            }
        }

        if (createdHome)
            logger.info(marker, "Create home directory " + homeDir);
        logger.info(marker, "Starting");

        Filter configurationFilter = getEnabledServicesFilter(configuration);

        RunLevelController runLevelController = serviceLocator.getService(RunLevelController.class);
        runLevelController.setThreadingPolicy(hk2Configuration.getThreadingPolicy());
        runLevelController.setMaximumUseableThreads(hk2Configuration.getMaximumUseableThreads());

        runLevelController.proceedTo(hk2Configuration.getRunLevel());

        List<ServiceHandle<?>> services = serviceLocator.getAllServiceHandles(configurationFilter);

        if (logger.isDebugEnabled()) {
            StringBuilder msg = new StringBuilder();
            msg.append("Services:");
            for (ServiceHandle<?> service : services) {
                msg.append('\n').append(service.getActiveDescriptor().getImplementation());
            }
            logger.debug(marker, msg);
        }

        logger.info(marker, "Started");
    }

    public ServiceLocator getServiceLocator() {
        return serviceLocator;
    }

    public void close() {

        if (!serviceLocator.isShutdown()) {
            logger.info(marker, "Stopping");
            RunLevelController runLevelController = serviceLocator.getService(RunLevelController.class);
            runLevelController.proceedTo(-1);
            serviceLocator.shutdown();
            logger.info(marker, "Stopped");
        }
    }

    protected Filter getEnabledServicesFilter(Configuration configuration) {
        return d ->
        {
            boolean result = Optional.ofNullable(d.getName())
                    .map(name -> configuration.getBoolean(name + ".enabled")
                            .orElseGet(() -> configuration.getBoolean("defaults.enabled").orElse(false)))
                    .orElse(true);
            return result;
        };
    }

    protected void populateServiceLocator(ServiceLocator serviceLocator, Configuration configuration, Hk2Configuration hk2Configuration, PopulatorPostProcessor populatorPostProcessor) throws MultiException {
        DynamicConfigurationService dcs = serviceLocator.getService(DynamicConfigurationService.class);

        DynamicConfiguration dynamicConfiguration = ServiceLocatorUtilities.createDynamicConfiguration(serviceLocator);
        dynamicConfiguration.addActiveDescriptor(BuilderHelper.createConstantDescriptor(configuration));
        dynamicConfiguration.addActiveDescriptor(BuilderHelper.createConstantDescriptor(this));
        dynamicConfiguration.addActiveDescriptor(DefaultTopicDistributionService.class);
        dynamicConfiguration.commit();

        Populator populator = dcs.getPopulator();

        try {
            populator.populate(new UniqueDescriptorFileFinder(new ClasspathDescriptorFileFinder(ClassLoader.getSystemClassLoader(), hk2Configuration.getDescriptorNames())),
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
