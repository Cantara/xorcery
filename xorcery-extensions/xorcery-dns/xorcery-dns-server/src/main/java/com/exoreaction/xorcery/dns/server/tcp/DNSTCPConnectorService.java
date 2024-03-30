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
package com.exoreaction.xorcery.dns.server.tcp;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.dns.server.DnsServerConfiguration;
import com.exoreaction.xorcery.dns.server.DnsServerService;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.apache.logging.log4j.spi.LoggerContext;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

@Service(name = "dns.server.tcp")
@RunLevel(4)
public class DNSTCPConnectorService {

    @Inject
    public DNSTCPConnectorService(Configuration configuration, Provider<Server> serverProvider, DnsServerService dnsServerService, LoggerContext loggerContext) {
        DnsServerConfiguration config = new DnsServerConfiguration(configuration.getConfiguration("dns.server"));

        Server server = serverProvider.get();
        if (server != null)
        {
            ServerConnector connector = new ServerConnector(server, new DNSTCPConnectionFactory(dnsServerService, loggerContext));
            connector.setIdleTimeout(config.getDNSTCPConfiguration().getIdleTimeout().toMillis());
            connector.setPort(config.getPort());
            connector.setName("DNS");
            server.addConnector(connector);
        } else {
            loggerContext.getLogger(getClass()).warn("Could not start DNS TCP connector, Jetty server is not available");
        }
    }
}
