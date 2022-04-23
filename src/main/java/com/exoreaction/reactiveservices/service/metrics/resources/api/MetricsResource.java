package com.exoreaction.reactiveservices.service.metrics.resources.api;

import com.codahale.metrics.*;
import com.exoreaction.reactiveservices.jaxrs.resources.JsonApiResource;
import com.exoreaction.reactiveservices.jsonapi.Attributes;
import com.exoreaction.reactiveservices.jsonapi.ResourceDocument;
import com.exoreaction.reactiveservices.jsonapi.ResourceObject;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import static com.exoreaction.reactiveservices.jsonapi.ResourceObjects.toResourceObjects;

@Path("api/metrics")
public class MetricsResource
        extends JsonApiResource {

    private MetricRegistry metricRegistry;

    @Inject
    public MetricsResource(MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
    }

    @GET
    @Produces(PRODUCES_JSON_API)
    public String registry() {
        return new ResourceDocument.Builder()
                .data(metricRegistry.getMetrics().entrySet().stream().map(entry ->
                        new ResourceObject.Builder("metric", entry.getKey())
                                .attributes(new Attributes.Builder()
                                        .attribute("type", toTypeName(entry.getValue().getClass()))
                                        .build()).build()).collect(toResourceObjects()))
                .build().toString();
    }

    private String toTypeName(Class<? extends Metric> metricClass) {
        if (Gauge.class.isAssignableFrom(metricClass)) {
            return "gauge";
        } else if (Meter.class.isAssignableFrom(metricClass)) {
            return "meter";
        } else if (Counter.class.isAssignableFrom(metricClass)) {
            return "counter";
        } else if (Timer.class.isAssignableFrom(metricClass)) {
            return "timer";
        } else if (Histogram.class.isAssignableFrom(metricClass)) {
            return "histogram";
        } else {
            return "unknown";
        }
    }

}
