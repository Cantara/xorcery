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
import dev.xorcery.jetty.client.providers.DnsLookupSocketAddressResolver;
import dev.xorcery.json.JsonMerger;
import io.opentelemetry.api.OpenTelemetry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.transport.HttpClientConnectionFactory;
import org.eclipse.jetty.client.transport.HttpClientTransportDynamic;
import org.eclipse.jetty.http2.client.HTTP2Client;
import org.eclipse.jetty.http2.client.transport.ClientConnectionFactoryOverHTTP2;
import org.eclipse.jetty.io.ClientConnectionFactory;
import org.eclipse.jetty.io.ClientConnector;
import org.eclipse.jetty.util.SocketAddressResolver;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ScheduledExecutorScheduler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class HttpClientFactory implements AutoCloseable {

    protected final Logger logger = LogManager.getLogger(getClass());
    private final JettyClientsConfiguration jettyClientsConfiguration;
    private final ApplicationConfiguration applicationConfiguration;
    private final Supplier<DnsLookupService> dnsLookup;
    private final ClientSslContextFactory clientSslContextFactory;
    private final OpenTelemetry openTelemetry;
    private final Map<String, HttpClient> clients = new ConcurrentHashMap<>();

    public HttpClientFactory(
            JettyClientsConfiguration jettyClientsConfiguration,
            ApplicationConfiguration applicationConfiguration,
            Supplier<DnsLookupService> dnsLookup,
            ClientSslContextFactory clientSslContextFactory,
            OpenTelemetry openTelemetry
    ) {
        this.jettyClientsConfiguration = jettyClientsConfiguration;
        this.applicationConfiguration = applicationConfiguration;
        this.dnsLookup = dnsLookup;
        this.clientSslContextFactory = clientSslContextFactory;
        this.openTelemetry = openTelemetry;
    }

    public HttpClient newHttpClient(String name){
        return clients.computeIfAbsent(name, clientName ->{
            JettyClientConfiguration jettyClientConfiguration = jettyClientsConfiguration.getClient(clientName).orElseThrow(()->new IllegalArgumentException("No HttpClient configuration found named:"+name));
            if (!clientName.equals("default")){
                // Base all custom configurations on default
                JettyClientConfiguration defaultClientConfiguration = jettyClientsConfiguration.getClient("default").orElseThrow(()->new IllegalArgumentException("No Jetty client configuration found named:default"));
                jettyClientConfiguration = new JettyClientConfiguration(new Configuration(new JsonMerger().apply(defaultClientConfiguration.context().json(), jettyClientConfiguration.context().json())));
            }

            if (!jettyClientConfiguration.isEnabled())
                throw new IllegalArgumentException("HttpClient "+clientName+" is not enabled");

            // Client setup
            ClientConnector connector = new ClientConnector();
            connector.setIdleTimeout(jettyClientConfiguration.getIdleTimeout());
            connector.setReusePort(jettyClientConfiguration.getReusePort());

            // HTTP 1.1
            ClientConnectionFactory.Info http1 = HttpClientConnectionFactory.HTTP11;

            ClientConnectionFactoryOverHTTP2.HTTP2 http2 = null;

            JettyHttp2Configuration http2Configuration = jettyClientConfiguration.getHTTP2Configuration();
            if (http2Configuration.isEnabled()) {
                // HTTP/2
                HTTP2Client http2Client = new HTTP2Client(connector);
                http2Client.setIdleTimeout(http2Configuration.getIdleTimeout());

                http2 = new ClientConnectionFactoryOverHTTP2.HTTP2(http2Client);
            }

            HttpClientTransportDynamic transport = null;
            JettyClientSslConfiguration jettyClientSslConfiguration = jettyClientConfiguration.getSSLConfiguration();
            if (jettyClientSslConfiguration.isEnabled()) {
                SslContextFactory.Client sslClientContextFactory = clientSslContextFactory.newClient(jettyClientConfiguration.getSSLConfiguration());
                if (sslClientContextFactory != null)
                    connector.setSslContextFactory(sslClientContextFactory);
            }

            // Figure out correct transport dynamics
            if (http2 != null) {
                transport = new HttpClientTransportDynamic(connector, http2, http1);
            } else {
                transport = new HttpClientTransportDynamic(connector, http1);
            }

            HttpClient client = new HttpClient(transport);
            client.getRequestListeners().addListener(new OpenTelemetryRequestListener(openTelemetry));
            client.setConnectTimeout(jettyClientConfiguration.getConnectTimeout().toMillis());
            client.setRequestBufferSize(jettyClientConfiguration.getRequestBufferSize());
            QueuedThreadPool executor = new JettyClientConnectorThreadPool();
            executor.setDaemon(true);
            executor.setName("jetty-http-client-"+clientName);
            client.setExecutor(executor);
            client.setScheduler(new ScheduledExecutorScheduler(applicationConfiguration.getName() + "-scheduler", false));

            if (dnsLookup.get() != null) {
                client.setSocketAddressResolver(new DnsLookupSocketAddressResolver(dnsLookup.get()));
            } else {
                client.setSocketAddressResolver(new SocketAddressResolver.Async(client.getExecutor(), client.getScheduler(), client.getAddressResolutionTimeout()));
            }

            try {
                client.start();
                logger.info("Started Jetty client");
            } catch (Exception e) {
                logger.warn("Could not start Jetty client", e);
                throw new RuntimeException(e);
            }
            return client;
        });
    }

    @Override
    public void close() throws Exception {
        for (HttpClient value : clients.values()) {
            value.stop();
        }
    }
}
