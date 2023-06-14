package com.exoreaction.xorcery.core;

import jakarta.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.spi.LoggerContext;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.Injectee;
import org.glassfish.hk2.api.InstantiationService;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

import java.io.IOException;

@Service
public class LoggerFactory
        implements Factory<Logger> {

    private final InstantiationService instantiationService;
    private final LoggerContext loggerContext;

    @Inject
    public LoggerFactory(InstantiationService instantiationService, LoggerContext loggerContext) throws IOException {
        this.instantiationService = instantiationService;
        this.loggerContext = loggerContext;
    }

    @Override
    @PerLookup
    public Logger provide() {

        Injectee injectee = instantiationService.getInstantiationData().getParentInjectee();
        if (injectee != null)
            if (injectee.getInjecteeDescriptor() != null) {
                String name = injectee.getInjecteeDescriptor().getImplementation();
                return loggerContext.getLogger(name);
            } else if (injectee.getInjecteeClass() != null) {
                return loggerContext.getLogger(injectee.getInjecteeClass());
            }

        return loggerContext.getLogger("xorcery");
    }

    @Override
    public void dispose(Logger instance) {
    }
}
