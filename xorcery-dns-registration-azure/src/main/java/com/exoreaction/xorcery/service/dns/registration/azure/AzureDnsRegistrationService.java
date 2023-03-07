package com.exoreaction.xorcery.service.dns.registration.azure;

import com.exoreaction.xorcery.configuration.model.Configuration;
import jakarta.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.client.HttpClient;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

@Service(name = "dns.registration")
@RunLevel(20)
public class AzureDnsRegistrationService implements PreDestroy {
    private final Logger logger = LogManager.getLogger(getClass());
    private final AzureDnsClient client;
    private final Configuration configuration;

    @Inject
    public AzureDnsRegistrationService(HttpClient httpClient, Configuration configuration) {
        this.client = new AzureDnsClient(httpClient);
        this.configuration = configuration;

        // register
    }

    @Override
    public void preDestroy() {
        // unregister
    }
}
