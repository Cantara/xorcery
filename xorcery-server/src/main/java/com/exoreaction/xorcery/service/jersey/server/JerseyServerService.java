package com.exoreaction.xorcery.service.jersey.server;

import com.codahale.metrics.MetricRegistry;
import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.configuration.model.StandardConfiguration;
import com.exoreaction.xorcery.jsonapi.model.Attributes;
import com.exoreaction.xorcery.jsonapi.model.ResourceObject;
import com.exoreaction.xorcery.service.jersey.server.resources.ServerApplication;
import com.fasterxml.jackson.databind.JsonNode;
import io.dropwizard.metrics.jetty11.InstrumentedHandler;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.Jetty;
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.glassfish.jersey.servlet.ServletContainer;
import org.jvnet.hk2.annotations.Service;

import java.io.IOException;

@Service
public class JerseyServerService
    implements Factory<ServletContextHandler>
{
    private final Logger logger = LogManager.getLogger(getClass());

    private final ServletContextHandler ctx;

    @Inject
    public JerseyServerService(
            Configuration configuration,
            Server server,
            ServiceLocator serviceLocator,
            Provider<MetricRegistry> metricRegistry) throws IOException {

        ctx = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        ctx.setAttribute("jersey.config.servlet.context.serviceLocator", serviceLocator);
        ctx.setContextPath("/");

        ServerApplication app = new ServerApplication();

        StandardConfiguration standardConfiguration = () -> configuration;
        ServiceLocatorUtilities.addOneConstant(serviceLocator, new ResourceObject.Builder("server", standardConfiguration.getId())
                .attributes(new Attributes.Builder()
                        .attribute("jetty.version", Jetty.VERSION)
                        .build()).build(), "server", ResourceObject.class);

        configuration.getList("jaxrs.register").ifPresent(jsonNodes ->
        {
            for (JsonNode jsonNode : jsonNodes) {
                try {
                    app.register(getClass().getClassLoader().loadClass(jsonNode.asText()));
                } catch (ClassNotFoundException e) {
                    logger.error("Could not load JAX-RS provider " + jsonNode.asText(), e);
                    throw new RuntimeException(e);
                }
            }
        });

        configuration.getList("jaxrs.packages").ifPresent(jsonNodes ->
        {
            for (JsonNode jsonNode : jsonNodes) {
                app.packages(jsonNode.asText());
            }
        });

        ServletContainer servletContainer = new ServletContainer(app);

        ServletHolder servletHolder = new ServletHolder(servletContainer);
        ctx.addServlet(servletHolder, "/*");
        servletHolder.setInitOrder(1);

        JettyWebSocketServletContainerInitializer.configure(ctx, null);

        Handler handler = ctx;
        if (configuration.getBoolean("metrics.enabled").orElse(false).equals(true))
        {
            InstrumentedHandler instrumentedHandler = new InstrumentedHandler(metricRegistry.get(), "jetty");
            instrumentedHandler.setHandler(ctx);
            handler = instrumentedHandler;
        }

        server.setHandler(handler);

        ServiceLocatorUtilities.addOneConstant(serviceLocator, servletContainer);
    }

    @Override
    @Singleton
    @Named("jersey")
    public ServletContextHandler provide() {
        return ctx;
    }

    @Override
    public void dispose(ServletContextHandler instance) {
        System.out.println("Dispose Jetty server");
    }
}
