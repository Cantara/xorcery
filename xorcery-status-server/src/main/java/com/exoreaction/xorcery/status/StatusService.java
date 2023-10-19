package com.exoreaction.xorcery.status;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.configuration.InstanceConfiguration;
import com.exoreaction.xorcery.server.api.ServiceResourceObject;
import com.exoreaction.xorcery.server.api.ServiceResourceObjects;
import jakarta.inject.Inject;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

@Service(name = "status")
@RunLevel(20)
public class StatusService {

    @Inject
    public StatusService(
            Configuration configuration,
            ServiceResourceObjects sro
    ) {
        sro.add(new ServiceResourceObject.Builder(InstanceConfiguration.get(configuration), "status")
                .with(b -> b.api("status", "api/status"))
                .build());
    }
}
