package com.exoreaction.reactiveservices.service.visualizer.resources.api;

import com.exoreaction.reactiveservices.service.visualizer.Visualizer;
import com.github.jknack.handlebars.Context;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import java.util.HashMap;
import java.util.Map;

@Path("api/visualizer")
public class VisualizerResource {
    private final Visualizer visualizer;

    @Inject
    public VisualizerResource(Visualizer visualizer) {
        this.visualizer = visualizer;
    }

    @GET
    public Context get() {
        Map<String, Object> root = new HashMap<>();

        root.put("services", visualizer.getServices());
        root.put("connections", visualizer.getConnections());

        return Context.newContext(root);
    }
}
