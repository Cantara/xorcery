package com.exoreaction.xorcery.service.certificates.ca;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.configuration.model.InstanceConfiguration;
import com.exoreaction.xorcery.server.api.ServiceResourceObjects;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import jakarta.inject.Inject;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

@Service(name = "intermediateca")
@RunLevel(4)
public class IntermediateCAService {

    @Inject
    public IntermediateCAService(ServiceResourceObjects sro, Configuration configuration) {
        sro.add(new ServiceResourceObject.Builder(new InstanceConfiguration(configuration.getConfiguration("instance")), "intermediateca")
                .with(b ->
                {
                    b.api("certificate", "api/ca/rootca.cer");
                    b.api("crl", "api/ca/intermediateca.crl");
                })
                .build());
    }
}
