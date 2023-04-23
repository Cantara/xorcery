package com.exoreaction.xorcery.service.jetty.server;


import com.exoreaction.xorcery.service.metricregistry.MetricRegistryWrapper;
import io.dropwizard.metrics.jetty11.InstrumentedHandler;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.IterableProvider;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

@Service(name = "jetty.server")
@RunLevel(4)
public class ServletContextHandlerFactory
    implements Factory<ServletContextHandler>
{
    private final ServletContextHandler servletContextHandler;

    @Inject
    public ServletContextHandlerFactory(Server server,
                                        ServiceLocator serviceLocator,
                                        IterableProvider<SecurityHandler> securityHandlerProvider,
                                        @Named("jetty") MetricRegistryWrapper jettyMetricRegistryWrapper) {
        servletContextHandler = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        servletContextHandler.setAttribute("jersey.config.servlet.context.serviceLocator", serviceLocator);
        servletContextHandler.setContextPath("/");

        JettyWebSocketServletContainerInitializer.configure(servletContextHandler, null);

        Handler handler = servletContextHandler;
        if (securityHandlerProvider.getHandle() != null)
        {
            SecurityHandler securityHandler = securityHandlerProvider.get();
            securityHandler.setHandler(handler);
            handler = securityHandler;
        }

        if (jettyMetricRegistryWrapper != null) {
            InstrumentedHandler instrumentedHandler = new InstrumentedHandler(jettyMetricRegistryWrapper.metricRegistry(), "");
            instrumentedHandler.setHandler(handler);
            handler = instrumentedHandler;
        }

        server.setHandler(handler);
    }

    @Override
    @Singleton
    @Named("jetty.server")
    public ServletContextHandler provide() {
        return servletContextHandler;
    }

    @Override
    public void dispose(ServletContextHandler instance) {
    }
}
