package com.exoreaction.xorcery.service.metrics.resources.api;

import com.codahale.metrics.*;
import com.exoreaction.xorcery.jsonapi.model.Attributes;
import com.exoreaction.xorcery.jsonapi.model.ResourceDocument;
import com.exoreaction.xorcery.jsonapi.model.ResourceObject;
import com.exoreaction.xorcery.jsonapi.resources.JsonApiResource;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import static com.exoreaction.xorcery.jaxrs.MediaTypes.PRODUCES_JSON_API_TEXT_HTML_YAML;
import static com.exoreaction.xorcery.jsonapi.model.ResourceObjects.toResourceObjects;

@Path("api/metrics")
public class MetricsResource
        extends JsonApiResource {

    private MetricRegistry metricRegistry;

    @Inject
    public MetricsResource(MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
    }

    @GET
    @Produces(PRODUCES_JSON_API_TEXT_HTML_YAML)
    public ResourceDocument metrics() {
        return new ResourceDocument.Builder()
                .data(metricRegistry.getMetrics().entrySet().stream().map(entry ->
                        new ResourceObject.Builder("metric", entry.getKey())
                                .attributes(new Attributes.Builder()
                                        .attribute("type", toTypeName(entry.getValue().getClass()))
                                        .build()).build()).collect(toResourceObjects()))
                .build();
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
