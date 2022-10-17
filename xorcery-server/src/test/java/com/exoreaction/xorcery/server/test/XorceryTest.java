package com.exoreaction.xorcery.server.test;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.configuration.builder.StandardConfigurationBuilder;
import com.exoreaction.xorcery.server.Xorcery;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class XorceryTest {

    @Test
    public void thatBasicWiringWorks() throws Exception {
        Configuration configuration = new Configuration.Builder().with(new StandardConfigurationBuilder()::addTestDefaults).build();
        try (Xorcery xorcery = new Xorcery(configuration)) {
            int httpPort = getHttpPort(xorcery.getServiceLocator().getService(Server.class));
            assertTrue(httpPort > 1024);
            System.out.printf("Jetty http port: %d%n", httpPort);
            int httpsPort = getHttpsPort(xorcery.getServiceLocator().getService(Server.class));
            assertTrue(httpsPort < 0 || httpsPort > 1024);
            System.out.printf("Jetty https port: %d%n", httpsPort);
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
