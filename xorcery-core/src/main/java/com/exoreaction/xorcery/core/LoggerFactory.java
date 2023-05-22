package com.exoreaction.xorcery.core;

import jakarta.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.Injectee;
import org.glassfish.hk2.api.InstantiationService;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

@Service
public class LoggerFactory
        implements Factory<Logger> {

    private final InstantiationService instantiationService;

    @Inject
    public LoggerFactory(InstantiationService instantiationService) {
        this.instantiationService = instantiationService;
        // TODO Do something clever here with configuration and LoggerContexts and whatnot
    }

    @Override
    @PerLookup
    public Logger provide() {

        Injectee injectee = instantiationService.getInstantiationData().getParentInjectee();
        if (injectee != null && injectee.getInjecteeDescriptor() != null) {
            String name = injectee.getInjecteeDescriptor().getImplementation();
            return LogManager.getLogger(name);
        }

        return LogManager.getLogger("xorcery");
    }

    @Override
    public void dispose(Logger instance) {
    }
}
