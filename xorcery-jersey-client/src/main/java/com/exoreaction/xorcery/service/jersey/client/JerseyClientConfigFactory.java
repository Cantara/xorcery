package com.exoreaction.xorcery.service.jersey.client;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.service.dns.client.api.DnsLookup;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.eclipse.jetty.client.HttpClient;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.InstantiationService;
import org.glassfish.hk2.api.IterableProvider;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jetty.connector.JettyConnectorProvider;
import org.glassfish.jersey.jetty.connector.JettyHttpClientSupplier;
import org.glassfish.jersey.logging.LoggingFeature;
import org.jvnet.hk2.annotations.Service;

import java.util.Iterator;

@Service
public class JerseyClientConfigFactory
        implements Factory<ClientConfig> {

    private HttpClient client;
    private Configuration configuration;
    private InstantiationService instantiationService;
    private IterableProvider<DnsLookup> dnsLookups;

    @Inject
    public JerseyClientConfigFactory(HttpClient client, Configuration configuration, InstantiationService instantiationService,
                                     IterableProvider<DnsLookup> dnsLookups) {
        this.client = client;
        this.configuration = configuration;
        this.instantiationService = instantiationService;
        this.dnsLookups = dnsLookups;
    }

    @Override
    @PerLookup
    public ClientConfig provide() {
        String loggerName = "default";
        if (instantiationService.getInstantiationData().getParentInjectee() != null) {
            Class<?> injecteeClass = instantiationService.getInstantiationData().getParentInjectee().getInjecteeClass();
            Named named = injecteeClass.getAnnotation(Named.class);
            if (named != null) {
                loggerName = "client." + named;
            } else {
                loggerName = injecteeClass.getName();
            }
        }

        ClientConfig clientConfig = new ClientConfig();
        configuration.getJson("client.properties").ifPresent(json ->
        {
            if (json instanceof ObjectNode on) {
                Iterator<String> fieldNames = on.fieldNames();
                while (fieldNames.hasNext()) {
                    String next = fieldNames.next();
                    clientConfig.property(next, on.get(next).textValue());
                }
            }
        });

        DnsLookup dnsLookup = dnsLookups.get();

        JettyConnectorProvider jettyConnectorProvider = new JettyConnectorProvider();
        String scheme = configuration.getBoolean("client.ssl.enabled").map(enabled -> enabled ? "https" : "http").orElse("http");
        return clientConfig
                .register(new LoggingFeature.LoggingFeatureBuilder().withLogger(java.util.logging.Logger.getLogger(loggerName)).build())
                .register(new JettyHttpClientSupplier(client))
                .connectorProvider(dnsLookup != null ? new SRVConnectorProvider(dnsLookup, scheme, jettyConnectorProvider) : jettyConnectorProvider);
    }

    @Override
    public void dispose(ClientConfig instance) {
    }
}
