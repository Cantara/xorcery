/*
 * Copyright © 2022 eXOReaction AS (rickard@exoreaction.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.exoreaction.xorcery.service.jersey.client;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.service.dns.client.DnsLookupService;
import com.exoreaction.xorcery.service.jetty.client.JettyClientSslConfiguration;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.ws.rs.client.ClientBuilder;
import org.eclipse.jetty.client.HttpClient;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.InstantiationService;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jetty.connector.JettyConnectorProvider;
import org.glassfish.jersey.jetty.connector.JettyHttpClientSupplier;
import org.glassfish.jersey.logging.LoggingFeature;
import org.jvnet.hk2.annotations.Service;

import java.util.Iterator;

@Service
public class ClientBuilderFactory
        implements Factory<ClientBuilder> {

    private HttpClient client;
    private Configuration configuration;
    private InstantiationService instantiationService;
    private Provider<DnsLookupService> dnsLookups;

    @Inject
    public ClientBuilderFactory(HttpClient client,
                                Configuration configuration,
                                InstantiationService instantiationService,
                                Provider<DnsLookupService> dnsLookups) {
        this.client = client;
        this.configuration = configuration;
        this.instantiationService = instantiationService;
        this.dnsLookups = dnsLookups;
    }

    @Override
    @PerLookup
    public ClientBuilder provide() {
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

        JerseyClientConfiguration jerseyClientConfiguration = new JerseyClientConfiguration(configuration.getConfiguration("jersey.client"));

        ClientConfig clientConfig = new ClientConfig();
        jerseyClientConfiguration.getProperties().ifPresent(json ->
        {
            if (json instanceof ObjectNode on) {
                Iterator<String> fieldNames = on.fieldNames();
                while (fieldNames.hasNext()) {
                    String next = fieldNames.next();
                    clientConfig.property(next, on.get(next).textValue());
                }
            }
        });

        DnsLookupService dnsLookup = dnsLookups.get();

        JettyConnectorProvider jettyConnectorProvider = new JettyConnectorProvider();
        JettyClientSslConfiguration sslConfiguration = new JettyClientSslConfiguration(configuration.getConfiguration("jetty.client.ssl"));
        String scheme = sslConfiguration.isEnabled() ? "https" : "http";

        return ClientBuilder.newBuilder()
                .withConfig(clientConfig
                        .connectorProvider(dnsLookup != null ? new SRVConnectorProvider(dnsLookup, scheme, jettyConnectorProvider) : jettyConnectorProvider))
                .register(new LoggingFeature.LoggingFeatureBuilder().withLogger(java.util.logging.Logger.getLogger(loggerName)).build())
                .register(new JettyHttpClientSupplier(client));
    }

    @Override
    public void dispose(ClientBuilder instance) {
    }
}
