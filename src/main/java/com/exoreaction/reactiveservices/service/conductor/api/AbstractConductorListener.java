package com.exoreaction.reactiveservices.service.conductor.api;

import com.exoreaction.reactiveservices.configuration.Configuration;
import com.exoreaction.reactiveservices.jsonapi.model.Link;
import com.exoreaction.reactiveservices.server.model.ServiceResourceObject;
import com.exoreaction.reactiveservices.service.conductor.resources.model.Group;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ServiceIdentifier;
import com.exoreaction.reactiveservices.service.registry.api.Registry;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractConductorListener
        implements ConductorListener {
    protected final ServiceIdentifier serviceIdentifier;
    private final String rel;
    private final Map<String, List<Link>> groupServiceConnections = new ConcurrentHashMap<>();
    private final Marker marker;
    private final Logger logger = LogManager.getLogger(getClass());

    public AbstractConductorListener(ServiceIdentifier serviceIdentifier, String rel) {
        this.serviceIdentifier = serviceIdentifier;
        this.marker = MarkerManager.getMarker(serviceIdentifier.toString());
        this.rel = rel;
    }

    public abstract void connect(ServiceResourceObject sro, Link link, Configuration sourceConfiguration, Configuration consumerConfiguration);

    @Override
    public void addedGroup(Group group, Registry registry) {
        updatedGroup(group, registry);
    }

    @Override
    public void updatedGroup(Group group, Registry registry) {
        if (group.getConsumers().stream().anyMatch(serviceIdentifier::equals)) {
            // Find publisher to connect to
            group.getSources()
                    .stream()
                    .map(registry::getService)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .forEach(sro ->
                    {
                        sro.getLinkByRel(rel).ifPresent(link ->
                        {
                            List<Link> current = groupServiceConnections.computeIfAbsent(group.resourceObject().getId(), id -> new ArrayList<>());
                            if (!current.contains(link))
                            {
                                current.add(link);
                                connect(sro, link, group.getSourceConfiguration(), group.getConsumerConfiguration());
                                logger.info(marker, "Connect {} to {}", serviceIdentifier, link.getHref());
                            }
                        });
                    });
        }
    }
}
