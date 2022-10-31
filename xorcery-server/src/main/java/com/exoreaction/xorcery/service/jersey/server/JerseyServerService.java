package com.exoreaction.xorcery.service.jersey.server;

import com.codahale.metrics.MetricRegistry;
import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.configuration.model.StandardConfiguration;
import com.exoreaction.xorcery.core.Xorcery;
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
import org.glassfish.hk2.api.*;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.glassfish.jersey.internal.inject.Bindings;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.servlet.ServletContainer;
import org.jvnet.hk2.annotations.Service;

import java.io.IOException;
import java.util.List;

@Service
@Named("jersey")
public class JerseyServerService
        implements PreDestroy {
    private final Logger logger = LogManager.getLogger(getClass());

    private final JerseyServletContainer servletContainer;

    @Inject
    public JerseyServerService(
            Configuration configuration,
            ServletContextHandler ctx,
            ServiceLocator serviceLocator) throws Exception {

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

        servletContainer = new JerseyServletContainer(app);
        ServletHolder servletHolder = new ServletHolder(servletContainer);
        servletHolder.setInitOrder(1);
        ctx.addServlet(servletHolder, "/*");
        System.out.println("Started Jersey servlet");
    }

    @Override
    public void preDestroy() {
        servletContainer.stop();
    }
}
