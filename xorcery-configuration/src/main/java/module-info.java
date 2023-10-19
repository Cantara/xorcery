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
open module xorcery.configuration {
    uses com.exoreaction.xorcery.configuration.spi.ConfigurationProvider;
    exports com.exoreaction.xorcery.configuration.builder;
    exports com.exoreaction.xorcery.configuration.providers;
    exports com.exoreaction.xorcery.configuration.spi;

    requires transitive xorcery.configuration.api;
    requires xorcery.status.spi;

    requires static org.glassfish.hk2.api;
    requires static jakarta.inject;

    requires com.fasterxml.jackson.dataformat.javaprop;
    requires com.fasterxml.jackson.dataformat.yaml;
    requires org.apache.logging.log4j;

    provides com.exoreaction.xorcery.configuration.spi.ConfigurationProvider with
            com.exoreaction.xorcery.configuration.providers.SystemPropertiesConfigurationProvider,
            com.exoreaction.xorcery.configuration.providers.EnvironmentVariablesConfigurationProvider,
            com.exoreaction.xorcery.configuration.providers.CalculatedConfigurationProvider;
}
