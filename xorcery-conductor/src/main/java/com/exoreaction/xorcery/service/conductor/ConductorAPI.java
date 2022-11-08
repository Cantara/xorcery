package com.exoreaction.xorcery.service.conductor;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.json.VariableResolver;
import com.exoreaction.xorcery.jsonapi.model.ResourceDocument;
import com.exoreaction.xorcery.jsonapi.model.ResourceObject;
import com.exoreaction.xorcery.server.api.ServiceResourceObjects;
import com.exoreaction.xorcery.server.model.ServerResourceDocument;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import com.exoreaction.xorcery.service.conductor.api.Group;
import com.exoreaction.xorcery.service.conductor.api.GroupTemplate;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.hk2.api.messaging.MessageReceiver;
import org.glassfish.hk2.api.messaging.SubscribeTo;
import org.glassfish.hk2.api.messaging.Topic;
import org.jvnet.hk2.annotations.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author rickardoberg
 * @since 15/04/2022
 */
@Service
@MessageReceiver
@Named(ConductorAPI.SERVICE_TYPE)
public class ConductorAPI {

    public static final String SERVICE_TYPE = "conductor";

    private final Logger logger = LogManager.getLogger(getClass());

    private ConductorService conductorService;

    @Inject
    public ConductorAPI(ServiceResourceObjects serviceResourceObjects,
                        ConductorService conductorService,
                        Configuration configuration) {
        this.conductorService = conductorService;

        ServiceResourceObject sro = new ServiceResourceObject.Builder(() -> configuration, SERVICE_TYPE)
                .api("conductor", "api/conductor")
                .websocket("conductorgroups", "ws/conductor/groups")
                .build();

        serviceResourceObjects.publish(sro);

/*
        sro.getLinkByRel("conductorgroups").ifPresent(link ->
        {
            reactiveStreams.publisher(link.getHrefAsUri().getPath(), cfg -> new ConductorPublisher(), ConductorPublisher.class);
        });
*/

//        registry.addRegistryListener(new ConductorRegistryListener());

    }

    public List<Group> getGroups()
    {
        return conductorService.getGroups().getGroups();
    }

    public List<GroupTemplate> getTemplates()
    {
        return conductorService.getGroupTemplates().getTemplates();
    }
}
