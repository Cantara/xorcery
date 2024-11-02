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
package dev.xorcery.server.test;

import dev.xorcery.configuration.Configuration;
import dev.xorcery.configuration.builder.ConfigurationBuilder;
import dev.xorcery.core.Xorcery;
import dev.xorcery.net.Sockets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ServerTest {

    Logger logger = LogManager.getLogger(getClass());

    String config = """
            jetty.client.enabled: true
            jetty.server.enabled: true    
            """;

    @Test
    public void thatBasicWiringWorks() throws Exception {
        Configuration configuration = new ConfigurationBuilder()
                .addTestDefaults()
                .addYaml(config)
                .builder()
                .add("jetty.server.http.port", Sockets.nextFreePort())
                .add("jetty.server.ssl.port", Sockets.nextFreePort())
                .build();
        try (Xorcery xorcery = new Xorcery(configuration)) {
            int httpPort = getHttpPort(xorcery.getServiceLocator().getService(Server.class));
            assertTrue(httpPort > 1024);
            logger.debug("Jetty http port: {}", httpPort);
            int httpsPort = getHttpsPort(xorcery.getServiceLocator().getService(Server.class));
            assertTrue(httpsPort < 0 || httpsPort > 1024);
            logger.debug("Jetty https port: {}", httpsPort);
        }
    }

    public int getHttpPort(Server server) {
        int port = -3;
        for (Connector connector : server.getConnectors()) {
            // the first connector should be the http connector
            ServerConnector serverConnector = (ServerConnector) connector;
            List<String> protocols = serverConnector.getProtocols();
            if (!protocols.contains("ssl") && (protocols.contains("http/1.1") || protocols.contains("h2c"))) {
                port = serverConnector.getLocalPort();
                break;
            }
        }
        return port;
    }

    public int getHttpsPort(Server server) {
        int port = -3;
        for (Connector connector : server.getConnectors()) {
            // the first connector should be the http connector
            ServerConnector serverConnector = (ServerConnector) connector;
            List<String> protocols = serverConnector.getProtocols();
            if (protocols.contains("ssl") && (protocols.contains("http/1.1") || protocols.contains("h2"))) {
                port = serverConnector.getLocalPort();
                break;
            }
        }
        return port;
    }

}
