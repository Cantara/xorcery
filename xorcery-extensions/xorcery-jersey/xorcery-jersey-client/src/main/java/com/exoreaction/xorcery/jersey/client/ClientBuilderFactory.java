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
package com.exoreaction.xorcery.jersey.client;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.dns.client.providers.DnsLookupService;
import com.exoreaction.xorcery.jersey.client.providers.SRVConnectorProvider;
import com.exoreaction.xorcery.keystores.KeyStores;
import com.exoreaction.xorcery.keystores.KeyStoresConfiguration;
import com.exoreaction.xorcery.secrets.Secrets;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.ws.rs.client.ClientBuilder;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.spi.LoggerContext;
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
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Service
public class ClientBuilderFactory
        implements Factory<ClientBuilder> {

    private Provider<HttpClient> client;
    private Configuration configuration;
    private InstantiationService instantiationService;
    private Provider<DnsLookupService> dnsLookups;
    private final Provider<KeyStores> keyStoresProvider;
    private final Provider<Secrets> secretsProvider;
    private final Logger logger;
    private final LoggerContext loggerContext;

    @Inject
    public ClientBuilderFactory(Provider<HttpClient> client,
                                Configuration configuration,
                                InstantiationService instantiationService,
                                Provider<DnsLookupService> dnsLookups,
                                Provider<KeyStores> keyStoresProvider,
                                Provider<Secrets> secretsProvider,
                                Logger logger,
                                LoggerContext loggerContext) {
        this.client = client;
        this.configuration = configuration;
        this.instantiationService = instantiationService;
        this.dnsLookups = dnsLookups;
        this.keyStoresProvider = keyStoresProvider;
        this.secretsProvider = secretsProvider;
        this.logger = logger;
        this.loggerContext = loggerContext;
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

        HttpClient httpClient = client.get();
        ClientBuilder builder = ClientBuilder.newBuilder()
                .withConfig(clientConfig
                        .connectorProvider(dnsLookup != null
                                ? new SRVConnectorProvider(dnsLookup, jettyConnectorProvider, loggerContext)
                                : jettyConnectorProvider))
                .connectTimeout(jerseyClientConfiguration.getConnectTimeout().toSeconds(), TimeUnit.SECONDS)
                .readTimeout(jerseyClientConfiguration.getReadTimeout().toSeconds(), TimeUnit.SECONDS)
                .register(new LoggingFeature.LoggingFeatureBuilder().withLogger(java.util.logging.Logger.getLogger(loggerName)).build())
                .register(new JettyHttpClientSupplier(httpClient));

        jerseyClientConfiguration.getKeyStoreName().ifPresentOrElse(name ->
        {
            KeyStoresConfiguration.get(configuration).getKeyStoreConfiguration(name).ifPresent(keyStoreConfiguration ->
            {
                KeyStores ks = Objects.requireNonNull(keyStoresProvider.get(), "KeyStores service not available");
                Secrets secrets = Objects.requireNonNull(secretsProvider.get(), "Secrets service not available");
                String password = keyStoreConfiguration.getPassword().map(secrets::getSecretString).orElse(null);
                builder.keyStore(Objects.requireNonNull(ks.getKeyStore(name), "No such KeyStore:"+name),password );
            });
        }, ()->
        {
            logger.warn("SSL enabled but no keystore specified");
        });
        jerseyClientConfiguration.getTrustStoreName().ifPresent(name ->
        {
            KeyStores ks = Objects.requireNonNull(keyStoresProvider.get(), "KeyStores service not available");
            builder.trustStore(Objects.requireNonNull(ks.getKeyStore(name), "No such KeyStore:"+name));
        });

        return builder;
    }

    @Override
    public void dispose(ClientBuilder instance) {
    }
}
