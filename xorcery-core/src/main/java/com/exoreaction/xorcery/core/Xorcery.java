package com.exoreaction.xorcery.core;

import com.exoreaction.xorcery.configuration.model.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.Filter;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.extras.events.internal.DefaultTopicDistributionService;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;

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
        implements AutoCloseable, PreDestroy {

    private static final Logger logger = LogManager.getLogger(Xorcery.class);

    private ServiceLocator serviceLocator;
    private List<Object> started;

    public Xorcery(Configuration configuration) throws Exception {
        this(configuration, ServiceLocatorUtilities.createAndPopulateServiceLocator(configuration.getString("name").orElse(null)));
    }

    public Xorcery(Configuration configuration, ServiceLocator serviceLocator) throws Exception {
        logger.info("Creating");
        this.serviceLocator = serviceLocator;
        ServiceLocatorUtilities.addOneConstant(serviceLocator, configuration);
        ServiceLocatorUtilities.addOneConstant(serviceLocator, this);
        ServiceLocatorUtilities.addClasses(serviceLocator, DefaultTopicDistributionService.class);

        // Instantiate all enabled services
        logger.info("Initializing");

        Filter configurationFilter = getEnabledServicesFilter(configuration);
        List<?> services = serviceLocator.getAllServices(configurationFilter);

        if (logger.isDebugEnabled()) {
            StringBuilder msg = new StringBuilder();
            msg.append("Services:");
            for (Object service : services) {
                msg.append('\n').append(service.toString());
            }
            logger.debug(msg);
        }
    }

    public ServiceLocator getServiceLocator() {
        return serviceLocator;
    }

    public void close() {

        logger.info("Stopping");
        serviceLocator.shutdown();
        logger.info("Stopped");
    }

    @Override
    public void preDestroy() {
        System.out.println("Xorcery preDestroy");
    }

    protected Filter getEnabledServicesFilter(Configuration configuration) {
        return d ->
        {
            boolean result = Optional.ofNullable(d.getName())
                    .map(name -> configuration.getBoolean(name + ".enabled")
                            .orElseGet(() -> configuration.getBoolean("defaults.enabled").orElse(false)))
                    .orElse(true);
            System.out.println("Filter:"+d.getImplementation()+":"+d.getName()+":"+result);
            return result;
        };
    }
}
