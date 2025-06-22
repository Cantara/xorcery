package dev.xorcery.kurrent.jsonapi.resources;

import dev.xorcery.configuration.Configuration;
import dev.xorcery.configuration.InstanceConfiguration;
import dev.xorcery.jsonapi.service.ServiceResourceObject;
import dev.xorcery.jsonapi.service.ServiceResourceObjects;
import jakarta.inject.Inject;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

@Service(name = "kurrent.jsonapi")
@RunLevel(6)
public class KurrentResourcesService {

    @Inject
    public KurrentResourcesService(ServiceResourceObjects serviceResourceObjects, Configuration configuration) {
        ServiceResourceObject sro = new ServiceResourceObject.Builder(InstanceConfiguration.get(configuration), "kurrent")
                .api("self", "api/kurrent")
                .build();
        serviceResourceObjects.add(sro);
    }
}
