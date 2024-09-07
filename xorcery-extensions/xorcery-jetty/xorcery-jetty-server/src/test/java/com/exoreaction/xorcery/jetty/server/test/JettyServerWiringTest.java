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
package com.exoreaction.xorcery.jetty.server.test;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.configuration.builder.ConfigurationBuilder;
import com.exoreaction.xorcery.core.Xorcery;
import com.exoreaction.xorcery.hk2.Services;
import org.apache.logging.log4j.LogManager;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.server.Server;
import org.glassfish.hk2.api.ServiceLocator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class JettyServerWiringTest {

    @Test
    void testServerStartup() throws Exception {

        Configuration configuration = new ConfigurationBuilder().addTestDefaults()
                .addYaml("""
                jetty.server.http.port: "{{ CALCULATED.dynamicPorts.http }}"
                jetty.server.ssl.port: "{{ CALCULATED.dynamicPorts.ssl }}"
                """)
                .build();

        try (Xorcery xorcery = new Xorcery(configuration))
        {
            LogManager.getLogger().info(configuration);
            ServiceLocator serviceLocator = xorcery.getServiceLocator();
            assertNotNull(serviceLocator.getService(Server.class));
            assertNotNull(Services.ofType(serviceLocator, ServletContextHandler.class).orElse(null));
        }
    }
}
