package com.exoreaction.reactiveservices.service.visualizer.resources.api;

import com.exoreaction.reactiveservices.jsonapi.ResourceObject;
import com.exoreaction.reactiveservices.service.visualizer.VisualizerService;
import com.github.jknack.handlebars.Context;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import java.util.HashMap;
import java.util.Map;

@Path("api/visualizer")
public class VisualizerResource {
    private final VisualizerService visualizerService;

    @Inject
    public VisualizerResource(VisualizerService visualizerService) {
        this.visualizerService = visualizerService;
    }

    @GET
    public Context get() {
        Map<String, Object> root = new HashMap<>();

        root.put("services", visualizerService.getServices());
        root.put("connections", visualizerService.getConnections());

        return Context.newContext(root);
    }
}
