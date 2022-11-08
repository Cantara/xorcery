package com.exoreaction.xorcery.service.registry;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.server.api.ServiceResourceObjects;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.api.messaging.Topic;
import org.jvnet.hk2.annotations.Service;

/**
 * @author rickardoberg
 * @since 13/04/2022
 */
@Service(name="registry")
public class RegistryService {
    public static final String SERVICE_TYPE = "registry";

    private static final Logger logger = LogManager.getLogger(RegistryService.class);

    @Inject
    public RegistryService(Configuration configuration,
                           ServiceResourceObjects serviceResourceObjects) {
        serviceResourceObjects.publish(new ServiceResourceObject.Builder(() -> configuration, SERVICE_TYPE)
                .api("registry", "api/registry")
                .websocket("registrysubscriber", "ws/registry/subscriber")
                .websocket("registrypublisher", "ws/registry/publisher")
                .build());
    }
}
