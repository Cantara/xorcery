package com.exoreaction.reactiveservices.service.log4jappender;

import com.exoreaction.reactiveservices.jaxrs.AbstractFeature;
import com.exoreaction.reactiveservices.jsonapi.Links;
import com.exoreaction.reactiveservices.jsonapi.ResourceObject;
import com.exoreaction.reactiveservices.server.Server;
import com.exoreaction.reactiveservices.service.helpers.ServiceResourceObjectBuilder;
import com.exoreaction.reactiveservices.service.log4jappender.resources.websocket.LogWebSocketServlet;
import jakarta.ws.rs.core.FeatureContext;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;

import java.net.URI;

@Provider
public class Log4jAppenderFeature
        extends AbstractFeature {
    @Override
    protected String serviceType() {
        return "log4jappender";
    }

    @Override
    protected void buildResourceObject(ServiceResourceObjectBuilder builder) {
                builder.websocket("logevents", "ws/logevents");
    }

    @Override
    protected void configure() {
        injectionManager.getInstance(ServletContextHandler.class).addServlet(new ServletHolder(new LogWebSocketServlet()), "/ws/logevents");

        context.register(Log4jAppenderEventPublisher.class, ContainerLifecycleListener.class);
    }
}
