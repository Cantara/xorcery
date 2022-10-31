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
        logger.info("Creating");
        serviceLocator = ServiceLocatorUtilities.createAndPopulateServiceLocator(configuration.getString("id").orElse(null));
        ServiceLocatorUtilities.addOneConstant(serviceLocator, configuration);
        ServiceLocatorUtilities.addOneConstant(serviceLocator, this);
        ServiceLocatorUtilities.addClasses(serviceLocator, DefaultTopicDistributionService.class);

        // Instantiate
        logger.info("Initializing");
        Filter configurationFilter = d -> !d.getAdvertisedContracts().contains(Configuration.class.getName());
        List<?> services = serviceLocator.getAllServices(configurationFilter);

        if (logger.isDebugEnabled()) {
            StringBuilder msg = new StringBuilder();
            msg.append("Services:");
            for (Object service : services) {
                msg.append('\n').append(service.toString());
            }
            logger.debug(msg);
        }

        // "Start" all services
        logger.info("Starting");
        started = new ArrayList<>();
/*
        for (Object service : services) {
            try {
                Method startMethod = service.getClass().getMethod("start");
                startMethod.invoke(service);
                started.add(service);
            } catch (NoSuchMethodException e) {
                // Not a startable service, ok!
            }
        }
*/
        logger.info("Started");
    }

    public ServiceLocator getServiceLocator() {
        return serviceLocator;
    }

    public void close() {

        logger.info("Stopping");
/*
        Collections.reverse(started);
        for (Object service : started) {
            try {
                Method stopMethod = service.getClass().getMethod("stop");
                stopMethod.invoke(service);
            } catch (NoSuchMethodException e) {
                // Not a stoppable service, ok!
            } catch (Throwable t) {
                logger.warn("Exception while stopping a service", t);
            }
        }
*/

        serviceLocator.shutdown();
        logger.info("Stopped");
    }

    @Override
    public void preDestroy() {
        System.out.println("Xorcery preDestroy");
    }
}
