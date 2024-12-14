package dev.xorcery.jetty.server.http2.test;

import dev.xorcery.configuration.Configuration;
import dev.xorcery.configuration.builder.ConfigurationBuilder;
import dev.xorcery.core.Xorcery;
import dev.xorcery.hk2.Services;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.server.Server;
import org.glassfish.hk2.api.ServiceLocator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class JettyHttp2ServerTest {
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
//            LogManager.getLogger().info(configuration);
            ServiceLocator serviceLocator = xorcery.getServiceLocator();
            assertNotNull(serviceLocator.getService(Server.class));
            assertNotNull(Services.ofType(serviceLocator, ServletContextHandler.class).orElse(null));
        }
    }
}
