package com.exoreaction.xorcery.core;

import com.exoreaction.xorcery.configuration.model.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.api.*;
import org.glassfish.hk2.extras.events.internal.DefaultTopicDistributionService;
import org.glassfish.hk2.utilities.BuilderHelper;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @author rickardoberg
 * @since 12/04/2022
 */
public class Xorcery
        implements AutoCloseable {

    private final Logger logger;

    private final ServiceLocator serviceLocator;

    public Xorcery(Configuration configuration) throws Exception {
        this(configuration, ServiceLocatorFactory.getInstance().create(configuration.getString("name").orElse(null)));
    }

    public Xorcery(Configuration configuration, ServiceLocator serviceLocator) throws Exception {
        System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");
        logger = LogManager.getLogger(Xorcery.class);

        this.serviceLocator = serviceLocator;
        populateServiceLocator(serviceLocator, configuration);

        // Instantiate all enabled services
        logger.info("Starting");

        Filter configurationFilter = getEnabledServicesFilter(configuration);

        List<?> services = serviceLocator.getAllServices(configurationFilter);

        if (logger.isDebugEnabled()) {
            StringBuilder msg = new StringBuilder();
            msg.append("Services:");
            for (Object service : services) {
                msg.append('\n').append(service.getClass().getName());
            }
            logger.debug(msg);
        }

        logger.info("Started");
    }

    public ServiceLocator getServiceLocator() {
        return serviceLocator;
    }

    public void close() {

        logger.info("Stopping");
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

    protected ServiceLocator populateServiceLocator(ServiceLocator serviceLocator, Configuration configuration) throws MultiException {
        DynamicConfigurationService dcs = serviceLocator.getService(DynamicConfigurationService.class);

        DynamicConfiguration dynamicConfiguration = ServiceLocatorUtilities.createDynamicConfiguration(serviceLocator);
        dynamicConfiguration.addActiveDescriptor(BuilderHelper.createConstantDescriptor(configuration));
        dynamicConfiguration.addActiveDescriptor(BuilderHelper.createConstantDescriptor(this));
        dynamicConfiguration.addActiveDescriptor(DefaultTopicDistributionService.class);
        dynamicConfiguration.commit();

        Populator populator = dcs.getPopulator();

        try {
            populator.populate();
        }
        catch (IOException e) {
            throw new MultiException(e);
        }

        return serviceLocator;
    }
}
