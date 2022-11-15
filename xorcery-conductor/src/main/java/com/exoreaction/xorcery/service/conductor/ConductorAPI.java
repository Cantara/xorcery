package com.exoreaction.xorcery.service.conductor;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.server.api.ServiceResourceObjects;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import com.exoreaction.xorcery.service.conductor.api.Group;
import com.exoreaction.xorcery.service.conductor.api.GroupTemplate;
import jakarta.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jvnet.hk2.annotations.Service;

import java.util.List;

/**
 * @author rickardoberg
 * @since 15/04/2022
 */
@Service(name=ConductorAPI.SERVICE_TYPE)
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
