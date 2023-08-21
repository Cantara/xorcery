package com.exoreaction.xorcery.neo4j.browser;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.configuration.InstanceConfiguration;
import com.exoreaction.xorcery.server.api.ServiceResourceObject;
import com.exoreaction.xorcery.server.api.ServiceResourceObjects;
import com.exoreaction.xorcery.util.Resources;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

@Service(name = "neo4jbrowser")
@RunLevel(4)
public class Neo4jBrowser {

    @Inject
    public Neo4jBrowser(ServiceResourceObjects serviceResourceObjects,
                        Configuration configuration,
                        Provider<ServletContextHandler> ctxProvider) throws URISyntaxException {

        // Install servlet for Neo4j Browser
        URL webRootLocation = Resources.getResource("browser/index.html").orElseThrow();
        URI webRootUri = URI.create(webRootLocation.toURI().toASCIIString().replaceFirst("/index.html", "/"));
        DefaultServlet defaultServlet = new DefaultServlet();
        ServletHolder servletHolder = new ServletHolder(defaultServlet);
        ServletContextHandler servletContextHandler = ctxProvider.get();
        servletContextHandler.setInitParameter(DefaultServlet.CONTEXT_INIT + "resourceBase", webRootUri.toASCIIString());
        servletContextHandler.setInitParameter(DefaultServlet.CONTEXT_INIT + "pathInfoOnly", Boolean.TRUE.toString());
        servletContextHandler.addServlet(servletHolder, "/api/neo4j/browser/*");

        serviceResourceObjects.add(new ServiceResourceObject.Builder(new InstanceConfiguration(configuration.getConfiguration("instance")), "neo4jbrowser")
                .api("browser", "api/neo4j/browser/")
                .build());
    }
}
