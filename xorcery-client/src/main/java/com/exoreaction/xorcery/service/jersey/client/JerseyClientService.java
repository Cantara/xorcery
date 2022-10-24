package com.exoreaction.xorcery.service.jersey.client;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.eclipse.jetty.client.HttpClient;
import org.glassfish.hk2.api.Factory;
import org.glassfish.jersey.jetty.connector.JettyHttpClientContract;
import org.glassfish.jersey.jetty.connector.JettyHttpClientSupplier;
import org.jvnet.hk2.annotations.Service;

@Service
public class JerseyClientService
        implements Factory<JettyHttpClientContract> {

    private HttpClient client;

    @Inject
    public JerseyClientService(HttpClient client) {
        this.client = client;
    }

    @Override
    @Singleton
    public JettyHttpClientContract provide() {
        return new JettyHttpClientSupplier(client);
    }

    @Override
    public void dispose(JettyHttpClientContract instance) {
        System.out.println("Dispose Jetty client");
    }
}
