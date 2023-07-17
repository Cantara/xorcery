package com.exoreaction.xorcery.dns.server.tcp;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.dns.server.DnsServerConfiguration;
import com.exoreaction.xorcery.dns.server.DnsServerService;
import jakarta.inject.Inject;
import org.apache.logging.log4j.spi.LoggerContext;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

@Service(name = "dns.server.tcp")
@RunLevel(4)
public class DNSTCPConnectorService {

    @Inject
    public DNSTCPConnectorService(Configuration configuration, Server server, DnsServerService dnsServerService, LoggerContext loggerContext) {
        DnsServerConfiguration config = new DnsServerConfiguration(configuration.getConfiguration("dns.server"));

        ServerConnector connector = new ServerConnector(server, new DNSTCPConnectionFactory(dnsServerService, loggerContext));
        connector.setIdleTimeout(config.getDNSTCPConfiguration().getIdleTimeout().toMillis());
        connector.setPort(config.getPort());
        connector.setName("DNS");

        server.addConnector(connector);
    }
}
