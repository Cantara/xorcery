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
import io.opentelemetry.api.OpenTelemetry;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

@Service(name = "jetty.client")
public class HttpClientFactoryHK2 extends HttpClientFactory
        implements Factory<HttpClient> {

    @Inject
    public HttpClientFactoryHK2(
            Configuration configuration,
            Provider<DnsLookupService> dnsLookup,
            Provider<SslContextFactory.Client> clientSslContextFactoryProvider,
            OpenTelemetry openTelemetry) throws Exception {
        super(
                new JettyClientConfiguration(configuration.getConfiguration("jetty.client")),
                ApplicationConfiguration.get(configuration),
                dnsLookup::get,
                clientSslContextFactoryProvider::get,
                openTelemetry
        );
    }

    @Override
    @PerLookup
    public HttpClient provide() {
        return super.provide();
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
}
