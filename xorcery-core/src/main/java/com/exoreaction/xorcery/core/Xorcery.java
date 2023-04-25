package com.exoreaction.xorcery.core;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.health.api.XorceryHealthCheckRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.api.*;
import org.glassfish.hk2.extras.events.internal.DefaultTopicDistributionService;
import org.glassfish.hk2.runlevel.RunLevelController;
import org.glassfish.hk2.utilities.BuilderHelper;
import org.glassfish.hk2.utilities.ClasspathDescriptorFileFinder;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Xorcery run level values:
 * 0: Configuration refresh
 * 2: Certificate refresh
 * 4: Servers
 * 6: Server publishers/subscribers
 * 8: Client publishers/subscribers
 * 18: Server start/stop
 * 20: DNS registration
 *
 * @author rickardoberg
 * @since 12/04/2022
 */
public class Xorcery
        implements AutoCloseable {

    private final Logger logger;

    private final ServiceLocator serviceLocator;

    public Xorcery(Configuration configuration) throws Exception {
        this(configuration, ServiceLocatorFactory.getInstance().create(null), "not-set");
    }

    public Xorcery(Configuration configuration, String applicationVersion) throws Exception {
        this(configuration, ServiceLocatorFactory.getInstance().create(null), applicationVersion);
    }

    public Xorcery(Configuration configuration, ServiceLocator serviceLocator) throws Exception {
        this(configuration, serviceLocator, "not-set");
    }

    public Xorcery(Configuration configuration, ServiceLocator serviceLocator, String applicationVersion) throws Exception {
        System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");
        logger = LogManager.getLogger(Xorcery.class);
        Hk2Configuration hk2Configuration = new Hk2Configuration(configuration.getConfiguration("hk2"));

        this.serviceLocator = serviceLocator;
        populateServiceLocator(serviceLocator, configuration);

        // Instantiate all enabled services
        logger.info("Starting");

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
            logger.debug(msg);
        }

        XorceryHealthCheckRegistry healthCheckService = serviceLocator.getService(XorceryHealthCheckRegistry.class);
        if (healthCheckService != null) {
            healthCheckService.setVersion(applicationVersion);
        } else {
            logger.warn("Unable to set application version, service for '{}.class' not found", XorceryHealthCheckRegistry.class.getSimpleName());
        }

        logger.info("Started");
    }

    public ServiceLocator getServiceLocator() {
        return serviceLocator;
    }

    public void close() {

        logger.info("Stopping");
        RunLevelController runLevelController = serviceLocator.getService(RunLevelController.class);
        runLevelController.proceedTo(0);
        serviceLocator.shutdown();
        logger.info("Stopped");
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

    protected void populateServiceLocator(ServiceLocator serviceLocator, Configuration configuration) throws MultiException {
        DynamicConfigurationService dcs = serviceLocator.getService(DynamicConfigurationService.class);

        DynamicConfiguration dynamicConfiguration = ServiceLocatorUtilities.createDynamicConfiguration(serviceLocator);
        dynamicConfiguration.addActiveDescriptor(BuilderHelper.createConstantDescriptor(configuration));
        dynamicConfiguration.addActiveDescriptor(BuilderHelper.createConstantDescriptor(this));
        dynamicConfiguration.addActiveDescriptor(DefaultTopicDistributionService.class);
        dynamicConfiguration.commit();

        Populator populator = dcs.getPopulator();

        try {
            populator.populate(new ClasspathDescriptorFileFinder(), new ConfigurationPostPopulatorProcessor(configuration));
        } catch (IOException e) {
            throw new MultiException(e);
        }
    }
}
