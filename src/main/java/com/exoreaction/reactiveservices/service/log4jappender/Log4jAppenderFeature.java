package com.exoreaction.reactiveservices.service.log4jappender;

import com.exoreaction.reactiveservices.jaxrs.AbstractFeature;
import com.exoreaction.reactiveservices.jsonapi.Links;
import com.exoreaction.reactiveservices.jsonapi.ResourceObject;
import com.exoreaction.reactiveservices.server.Server;
import com.exoreaction.reactiveservices.service.log4jappender.resources.LogWebSocketServlet;
import jakarta.ws.rs.core.FeatureContext;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.internal.inject.InjectionManager;

import java.net.URI;

@Provider
public class Log4jAppenderFeature
        extends AbstractFeature {

    @Override
    public boolean configure(FeatureContext context, InjectionManager injectionManager, Server server) {
        server.addService(new ResourceObject.Builder("service", "log4jappender")
                .links(
                        new Links.Builder().link("logevents", URI.create("ws://localhost:8080/ws/logevents")).build())
                .build());
        injectionManager.getInstance(ServletContextHandler.class).addServlet(new ServletHolder(new LogWebSocketServlet()), "/ws/logevents");
        return true;
    }
}
