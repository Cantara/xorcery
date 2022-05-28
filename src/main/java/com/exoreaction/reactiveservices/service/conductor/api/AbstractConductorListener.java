package com.exoreaction.reactiveservices.service.conductor.api;

import com.exoreaction.reactiveservices.jsonapi.model.Link;
import com.exoreaction.reactiveservices.service.conductor.resources.model.Group;
import com.exoreaction.reactiveservices.server.model.ServiceAttributes;
import com.exoreaction.reactiveservices.server.model.ServiceResourceObject;
import com.exoreaction.reactiveservices.service.reactivestreams.api.ServiceIdentifier;
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

    public abstract void connect(ServiceResourceObject sro, Link link, Optional<ServiceAttributes> attributes, Optional<ServiceAttributes> selfAttributes);

    @Override
    public void addedGroup(Group group) {
        // Does the added group contain this service?
        if (group.contains(serviceIdentifier)) {
            // Find publisher to connect to
            group.servicesByLinkRel(rel, (sro, attributes) ->
            {
                Optional<ServiceAttributes> selfAttributes = group.serviceAttributes(serviceIdentifier);
                sro.getLinkByRel(rel).ifPresent(link ->
                {
                    groupServiceConnections.computeIfAbsent(group.group().getId(), id -> new ArrayList<>())
                            .add(link);
                    connect(sro, link, attributes, selfAttributes);
                    logger.info(marker, "Connect {} to {}", serviceIdentifier, link.getHref());
                });
            });
        }
    }

    @Override
    public void updatedGroup(Group group) {
        // Does the updated group contain this service?
        if (group.contains(serviceIdentifier)) {
            // Find publisher to connect to
            group.servicesByLinkRel(rel, (sro, attributes) ->
            {
                Optional<ServiceAttributes> selfAttributes = group.serviceAttributes(serviceIdentifier);
                sro.getLinkByRel(rel).ifPresent(link ->
                {
                    // Check against existing connections
                    List<Link> current = groupServiceConnections.computeIfAbsent(group.group().getId(), id -> new ArrayList<>());
                    if (!current.contains(link)) {
                        current.add(link);
                        connect(sro, link, attributes, selfAttributes);
                        logger.info(marker, "Connect {} to {}", serviceIdentifier, link.getHref());
                    }
                });
            });
        }
    }
}
