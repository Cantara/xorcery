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
package dev.xorcery.jetty.client;

import dev.xorcery.configuration.ApplicationConfiguration;
import dev.xorcery.configuration.Configuration;
import dev.xorcery.dns.client.providers.DnsLookupService;
import dev.xorcery.hk2.Instances;
import io.opentelemetry.api.OpenTelemetry;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.eclipse.jetty.client.HttpClient;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.InstantiationService;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.PreDestroy;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

import java.util.Optional;

@Service(name = "jetty.client")
@ContractsProvided({Factory.class, HttpClientFactory.class})
public class HttpClientFactoryHK2 extends HttpClientFactory
        implements Factory<HttpClient>, PreDestroy {

    private final InstantiationService instantiationService;

    @Inject
    public HttpClientFactoryHK2(
            Configuration configuration,
            Provider<DnsLookupService> dnsLookup,
            ClientSslContextFactory clientSslContextFactory,
            OpenTelemetry openTelemetry,
            InstantiationService instantiationService) {
        super(
                new JettyClientsConfiguration(configuration.getConfiguration("jetty")),
                ApplicationConfiguration.get(configuration),
                dnsLookup::get,
                clientSslContextFactory,
                openTelemetry
        );
        this.instantiationService = instantiationService;
    }

    @Override
    @PerLookup
    public HttpClient provide() {
        String name = Optional.ofNullable(Instances.name(instantiationService)).orElse("default");
        return newHttpClient(name);
    }

    @Override
    public void dispose(HttpClient instance) {
        logger.info("Stopping Jetty client");
        try {
            instance.stop();
        } catch (Exception e) {
            logger.warn("Could not stop Jetty client", e);
        }
    }

    @Override
    public void preDestroy() {
        try {
            super.close();
        } catch (Exception e) {
            // Ignore
        }
    }
}
