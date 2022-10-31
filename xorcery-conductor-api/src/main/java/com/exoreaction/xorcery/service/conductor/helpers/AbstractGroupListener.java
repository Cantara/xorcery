package com.exoreaction.xorcery.service.conductor.helpers;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.jsonapi.model.Link;
import com.exoreaction.xorcery.server.model.ServiceIdentifier;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import com.exoreaction.xorcery.service.conductor.api.Group;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.MarkerManager;
import org.glassfish.hk2.api.messaging.MessageReceiver;
import org.glassfish.hk2.api.messaging.SubscribeTo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractGroupListener
    implements GroupListener
{
    protected final ServiceIdentifier serviceIdentifier;
    private final String rel;
    private final Map<String, List<Link>> groupServiceConnections = new ConcurrentHashMap<>();
    private final Logger logger = LogManager.getLogger(getClass());

    public AbstractGroupListener(ServiceIdentifier selfServiceIdentifier, String rel) {
        this.serviceIdentifier = selfServiceIdentifier;
        this.rel = rel;
    }

    public abstract void connect(ServiceResourceObject sro, Link link, Configuration sourceConfiguration, Configuration consumerConfiguration);

    @Override
    public void group(@SubscribeTo Group group) {
        // Client is consumer
        if (group.getConsumers().stream().anyMatch(serviceIdentifier::equals)) {
            // Find publisher to connect to
            group.getSources()
                    .stream()
                    .map(group::getServiceResourceObject)
                    .forEach(sro ->
                    {
                        sro.getLinkByRel(group.getConsumerConfiguration().getString("rel").orElse(rel)).ifPresent(link ->
                        {
                            List<Link> current = groupServiceConnections.computeIfAbsent(group.resourceObject().getId(), id -> new ArrayList<>());
                            if (!current.contains(link)) {
                                current.add(link);
                                connect(sro, link, group.getSourceConfiguration(), group.getConsumerConfiguration());
                                logger.info(MarkerManager.getMarker(link.getHref()), "Connect {} to {}", serviceIdentifier, link.getHref());
                            }
                        });
                    });
        }

        // Client is publisher
        if (group.getSources().stream().anyMatch(serviceIdentifier::equals)) {
            // Find consumer to connect to
            group.getConsumers()
                    .stream()
                    .map(group::getServiceResourceObject)
                    .forEach(sro ->
                    {
                        sro.getLinkByRel(group.getSourceConfiguration().getString("rel").orElse(rel)).ifPresent(link ->
                        {
                            List<Link> current = groupServiceConnections.computeIfAbsent(group.resourceObject().getId(), id -> new ArrayList<>());
                            if (!current.contains(link)) {
                                current.add(link);
                                connect(sro, link, group.getSourceConfiguration(), group.getConsumerConfiguration());
                                logger.info(MarkerManager.getMarker(link.getHref()), "Connect {} to {}", serviceIdentifier, link.getHref());
                            }
                        });
                    });
        }
    }
}
