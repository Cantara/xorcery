package com.exoreaction.xorcery.service.dns;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.server.api.ServiceResourceObjects;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import jakarta.inject.Inject;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

@Service
@RunLevel(6)
public class ServiceTester {

    @Inject
    public ServiceTester(Configuration configuration,
                         ServiceResourceObjects sro) {
        sro.add(new ServiceResourceObject.Builder(() -> configuration, "servicetest")
                .attribute("foo", "bar")
                .api("foorel", "somepath")
                .build());
    }
}
