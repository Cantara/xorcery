package com.exoreaction.xorcery.service.jersey.server;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.configuration.model.StandardConfiguration;
import com.exoreaction.xorcery.jsonapi.model.Attributes;
import com.exoreaction.xorcery.jsonapi.model.ResourceObject;
import com.exoreaction.xorcery.service.jersey.server.resources.ServerApplication;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.Jetty;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.jvnet.hk2.annotations.Service;

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
                    logger.debug("Registered " + jsonNode.asText());
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
    }

    @Override
    public void preDestroy() {
        servletContainer.stop();
    }
}
