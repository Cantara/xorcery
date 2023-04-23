package com.exoreaction.xorcery.admin.servlet;

import com.codahale.metrics.MetricRegistry;
import com.exoreaction.xorcery.configuration.builder.StandardConfigurationBuilder;
import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.core.Xorcery;
import com.exoreaction.xorcery.service.jersey.server.JerseyServerService;
import com.exoreaction.xorcery.service.metricregistry.MetricRegistryWrapper;
import com.exoreaction.xorcery.util.Sockets;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.api.ServiceLocator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AdminServletWiringTest {

    @Test
    void thatServletContextCanBeWiredByHK2() throws Exception {
        Configuration.Builder builder = new Configuration.Builder();
        new StandardConfigurationBuilder().addTestDefaults(builder);
        Configuration configuration = builder.add("id", "xorcery2")
                .add("host", "Bd35HecvTTB.xorcery.test")
                .add("jetty.server.http.port", Sockets.nextFreePort())
                .add("jetty.server.ssl.enabled", false)
                .add("jetty.client.ssl.enabled", false)
                //.add("hk2.threadPolicy", "USE_NO_THREADS")
                .add("hk2.runLevel", "20")
                .build();

        Xorcery xorcery = new Xorcery(configuration);
        ServiceLocator serviceLocator = xorcery.getServiceLocator();

        assertNotNull(serviceLocator.getService(Server.class));
        assertNotNull(serviceLocator.getService(ServletContextHandler.class));
        assertNotNull(serviceLocator.getService(XorceryAdminServletConfigurator.class));
        assertNotNull(serviceLocator.getService(JerseyServerService.class));
        assertTrue(serviceLocator.getService(Server.class).isStarted());
        MetricRegistry defaultMetricRegistry = serviceLocator.getService(MetricRegistry.class);
        MetricRegistryWrapper appMetricRegistry = serviceLocator.getService(MetricRegistryWrapper.class, "app");
        MetricRegistryWrapper rootMetricRegistry = serviceLocator.getService(MetricRegistryWrapper.class, "root");
        MetricRegistryWrapper xorceryMetricRegistry = serviceLocator.getService(MetricRegistryWrapper.class, "xorcery");
        MetricRegistryWrapper jvmMetricRegistry = serviceLocator.getService(MetricRegistryWrapper.class, "jvm");
        MetricRegistryWrapper jettyMetricRegistry = serviceLocator.getService(MetricRegistryWrapper.class, "jetty");
        MetricRegistryWrapper jerseyMetricRegistry = serviceLocator.getService(MetricRegistryWrapper.class, "jersey");
        ServiceHandle<MetricRegistry> defaultMetricRegistryHandle = serviceLocator.getServiceHandle(MetricRegistry.class);
        MetricRegistry another = defaultMetricRegistryHandle.getService();
        assertNotNull(appMetricRegistry);
        assertNotNull(defaultMetricRegistry);
        assertTrue(appMetricRegistry.metricRegistry() == defaultMetricRegistry);
    }
}
