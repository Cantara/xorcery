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
package dev.xorcery.runner;

import dev.xorcery.configuration.Configuration;
import dev.xorcery.core.Xorcery;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.Callable;

/**
 * Subclass this in your own project to provide more specific annotations for your commandline arguments.
 */
public class BaseMain
        implements Callable<Integer> {

    private Configuration configuration;
    private volatile Xorcery xorcery;

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public Xorcery getXorcery() {
        return xorcery;
    }

    @Override
    public Integer call() throws Exception {
        if (configuration == null)
        {
            throw new IllegalStateException("No configuration set");
        }

        Xorcery xorcery = new Xorcery(configuration);
        this.xorcery = xorcery;

        if (configuration.getBoolean("runner.keepRunning").orElse(true))
        {
            Logger mainLogger = LogManager.getLogger(BaseMain.class);
            Runtime.getRuntime().addShutdownHook(new Thread(() ->
            {
                mainLogger.info("Shutting down");
                try {
                    xorcery.close();
                } catch (Exception e) {
                    mainLogger.warn("Error during shutdown", e);
                }
            }));

            xorcery.getClosed().join();
            mainLogger.info("Shutdown");
        } else {
            xorcery.close();
        }
        LogManager.shutdown();

        return 0;
    }
}
