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

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.spi.LoggerContext;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.Injectee;
import org.glassfish.hk2.api.InstantiationService;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

import java.io.IOException;
import java.util.Optional;

@Service
public class LoggerFactory
        implements Factory<Logger> {

    private final InstantiationService instantiationService;
    private final LoggerContext loggerContext;

    @Inject
    public LoggerFactory(InstantiationService instantiationService, Provider<LoggerContext> loggerContextProvider) throws IOException {
        this.instantiationService = instantiationService;
        this.loggerContext = Optional.ofNullable(loggerContextProvider.get())
                .orElseGet(()->LogManager.getContext(Xorcery.class.getClassLoader(), false));
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
