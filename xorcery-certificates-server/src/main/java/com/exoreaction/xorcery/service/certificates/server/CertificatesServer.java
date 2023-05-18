package com.exoreaction.xorcery.service.certificates.server;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.configuration.model.InstanceConfiguration;
import com.exoreaction.xorcery.server.api.ServiceResourceObjects;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import jakarta.inject.Inject;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

@Service(name = "certificates.server")
@RunLevel(value = 2)
public class CertificatesServer {

    @Inject
    public CertificatesServer(ServiceResourceObjects serviceResourceObjects,
                              Configuration configuration
    ) {
        serviceResourceObjects.add(new ServiceResourceObject.Builder(new InstanceConfiguration(configuration.getConfiguration("instance")), "certificates")
                .with(b ->
                {
                    b.api("request", "api/certificates/request");
                })
                .build());
    }
}
