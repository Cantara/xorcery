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
package dev.xorcery.dns.server.tcp;

import dev.xorcery.dns.server.DnsServerService;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.spi.LoggerContext;
import org.eclipse.jetty.io.Connection;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.server.AbstractConnectionFactory;
import org.eclipse.jetty.server.Connector;

public class DNSTCPConnectionFactory
        extends AbstractConnectionFactory {
    private final DnsServerService dnsServerService;
    private final LoggerContext loggerContext;
    private final Logger logger;

    public DNSTCPConnectionFactory(DnsServerService dnsServerService, LoggerContext loggerContext) {
        super("DNS");
        this.dnsServerService = dnsServerService;
        this.loggerContext = loggerContext;
        logger = loggerContext.getLogger(getClass());
    }

    @Override
    public Connection newConnection(Connector connector, EndPoint endPoint) {
        logger.debug("newConnection {}", endPoint.getRemoteSocketAddress().toString());

        DNSTCPConnection connection = new DNSTCPConnection(endPoint, connector.getExecutor(), dnsServerService, loggerContext.getLogger(DNSTCPConnection.class));
        // Call configure() to apply configurations common to all connections.
        return configure(connection, connector, endPoint);
    }
}
