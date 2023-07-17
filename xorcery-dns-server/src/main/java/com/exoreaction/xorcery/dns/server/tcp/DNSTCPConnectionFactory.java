package com.exoreaction.xorcery.dns.server.tcp;

import com.exoreaction.xorcery.dns.server.DnsServerService;
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
