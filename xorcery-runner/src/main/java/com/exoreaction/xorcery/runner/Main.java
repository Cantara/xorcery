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
package com.exoreaction.xorcery.runner;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.configuration.ConfigurationLogger;
import com.exoreaction.xorcery.configuration.builder.ConfigurationBuilder;
import com.exoreaction.xorcery.core.Xorcery;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import picocli.CommandLine;

import java.io.File;
import java.util.concurrent.Callable;

/**
 * @author rickardoberg
 * @since 12/04/2022
 */

@CommandLine.Command(name = "xorcery", versionProvider = ConfigurationVersionProvider.class)
public class Main
        implements Callable<Integer> {

    public static void main(String[] args) {
        System.exit(new CommandLine(new Main()).execute(args));
    }

    @CommandLine.Parameters(index = "0", arity = "0..1", description = "Configuration file")
    private File configuration;

    @CommandLine.Option(names = "-id", description = "Server id")
    private String id;

    private volatile Xorcery xorcery;

    @Override
    public Integer call() throws Exception {
        Configuration configuration = loadConfiguration();

        Xorcery xorcery = new Xorcery(configuration);
        this.xorcery = xorcery;
        Logger mainLogger = LogManager.getLogger(Main.class);
        Runtime.getRuntime().addShutdownHook(new Thread(() ->
        {
            mainLogger.info("Shutting down");
            try {
                xorcery.close();
            } catch (Exception e) {
                mainLogger.warn("Error during shutdown", e);
            }
        }));

        synchronized (xorcery)
        {
            xorcery.wait();
        }
        mainLogger.info("Shutdown");
        LogManager.shutdown();

        return 0;
    }

    public Configuration loadConfiguration() {
        ConfigurationBuilder builder = new ConfigurationBuilder().addDefaults().addFile(configuration);

        // Log final and resolved configuration
        ConfigurationLogger.getLogger().log(builder.builder().toString());
        Configuration configuration = builder.build();
        ConfigurationLogger.getLogger().log(configuration.toString());

        return configuration;
    }

    public Xorcery getXorcery() {
        return xorcery;
    }
}
