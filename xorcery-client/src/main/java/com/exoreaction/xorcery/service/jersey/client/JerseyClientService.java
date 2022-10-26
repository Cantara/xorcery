package com.exoreaction.xorcery.service.jersey.client;

import com.exoreaction.xorcery.jsonapi.jaxrs.providers.JsonElementMessageBodyReader;
import com.exoreaction.xorcery.jsonapi.jaxrs.providers.JsonElementMessageBodyWriter;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.eclipse.jetty.client.HttpClient;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.InstantiationService;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jetty.connector.JettyConnectorProvider;
import org.glassfish.jersey.jetty.connector.JettyHttpClientSupplier;
import org.glassfish.jersey.logging.LoggingFeature;
import org.jvnet.hk2.annotations.Service;

@Service
public class JerseyClientService
        implements Factory<ClientConfig> {

    private HttpClient client;
    private InstantiationService instantiationService;

    @Inject
    public JerseyClientService(HttpClient client, InstantiationService instantiationService) {
        this.client = client;
        this.instantiationService = instantiationService;
    }

    @Override
    @Singleton
    public ClientConfig provide() {
        String loggerName = "default";
        if (instantiationService.getInstantiationData().getParentInjectee() != null)
        {
            Class<?> injecteeClass = instantiationService.getInstantiationData().getParentInjectee().getInjecteeClass();
            Named named = injecteeClass.getAnnotation(Named.class);
            if (named != null)
            {
                loggerName = "client."+named;
            } else
            {
                loggerName = injecteeClass.getName();
            }
        }
        return new ClientConfig()
                        .register(new JsonElementMessageBodyReader())
                        .register(new JsonElementMessageBodyWriter())
                        .register(new LoggingFeature.LoggingFeatureBuilder().withLogger(java.util.logging.Logger.getLogger(loggerName)).build())
                        .register(new JettyHttpClientSupplier(client))
                        .connectorProvider(new JettyConnectorProvider());
    }

    @Override
    public void dispose(ClientConfig instance) {
    }
}
