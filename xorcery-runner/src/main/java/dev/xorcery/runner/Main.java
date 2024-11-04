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
import dev.xorcery.configuration.ConfigurationLogger;
import dev.xorcery.configuration.builder.ConfigurationBuilder;
import picocli.CommandLine;

import java.io.File;

/**
 * Default Main which loads an optional given configuration file and starts the app.
 *
 * @author rickardoberg
 * @since 12/04/2022
 */
@CommandLine.Command(name = "xorcery", versionProvider = ConfigurationVersionProvider.class)
public class Main
    extends BaseMain {

    public static void main(String[] args) {
        System.exit(new CommandLine(new Main()).execute(args));
    }

    @CommandLine.Parameters(index = "0", arity = "0..1", description = "Configuration file")
    private File configuration;

    @Override
    public Integer call() throws Exception {
        setConfiguration(loadConfiguration());
        return super.call();
    }

    public Configuration loadConfiguration() {
        ConfigurationBuilder builder = new ConfigurationBuilder().addDefaults().addFile(configuration);

        // Log final and resolved configuration
        ConfigurationLogger.getLogger().log(builder.builder().toString());
        Configuration configuration = builder.build();
        ConfigurationLogger.getLogger().log(configuration.toString());

        return configuration;
    }
}
