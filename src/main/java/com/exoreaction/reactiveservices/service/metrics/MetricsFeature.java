package com.exoreaction.reactiveservices.service.metrics;

import com.codahale.metrics.MetricRegistry;
import com.exoreaction.reactiveservices.jaxrs.AbstractFeature;
import com.exoreaction.reactiveservices.jsonapi.Links;
import com.exoreaction.reactiveservices.jsonapi.ResourceObject;
import com.exoreaction.reactiveservices.server.Server;
import com.exoreaction.reactiveservices.service.log4jappender.resources.LogWebSocketServlet;
import com.exoreaction.reactiveservices.service.metrics.resources.websocket.MetricsWebSocketServlet;
import jakarta.ws.rs.core.FeatureContext;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.internal.inject.InjectionManager;

import java.net.URI;

@Provider
public class MetricsFeature
        extends AbstractFeature {
    @Override
    public boolean configure(FeatureContext context, InjectionManager im, Server server) {
        server.addService(new ResourceObject.Builder("service", "metrics")
                .links(
                        new Links.Builder()
                                .link("metrics", "http://localhost:8080/api/metrics")
                                .link("metricevents", "ws://localhost:8080/ws/metricevents?metrics={metric_names}").build())
                .build());
        MetricRegistry metricRegistry = im.getInstance(MetricRegistry.class);
        im.getInstance(ServletContextHandler.class)
                .addServlet(new ServletHolder(new MetricsWebSocketServlet(metricRegistry)), "/ws/metricevents");
        return true;
    }

}
