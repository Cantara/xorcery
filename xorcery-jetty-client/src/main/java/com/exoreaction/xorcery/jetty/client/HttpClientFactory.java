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
package com.exoreaction.xorcery.jetty.client;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.dns.client.providers.DnsLookupService;
import com.exoreaction.xorcery.jetty.client.providers.DnsLookupSocketAddressResolver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.RoundRobinConnectionPool;
import org.eclipse.jetty.client.dynamic.HttpClientTransportDynamic;
import org.eclipse.jetty.client.http.HttpClientConnectionFactory;
import org.eclipse.jetty.http2.client.HTTP2Client;
import org.eclipse.jetty.http2.client.http.ClientConnectionFactoryOverHTTP2;
import org.eclipse.jetty.io.ClientConnectionFactory;
import org.eclipse.jetty.io.ClientConnector;
import org.eclipse.jetty.util.SocketAddressResolver;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ScheduledExecutorScheduler;

import java.util.function.Supplier;

public class HttpClientFactory {

    private final Logger logger = LogManager.getLogger(getClass());
    private HttpClient client;

    public HttpClientFactory(Configuration configuration, Supplier<DnsLookupService> dnsLookup, Supplier<SslContextFactory.Client> clientSslContextFactoryProvider) throws Exception {

        JettyClientConfiguration jettyClientConfiguration = new JettyClientConfiguration(configuration.getConfiguration("jetty.client"));

        // Client setup
        ClientConnector connector = new ClientConnector();
        connector.setIdleTimeout(jettyClientConfiguration.getIdleTimeout());

        // HTTP 1.1
        ClientConnectionFactory.Info http1 = HttpClientConnectionFactory.HTTP11;

        ClientConnectionFactoryOverHTTP2.HTTP2 http2 = null;

        JettyHttp2Configuration http2Configuration = new JettyHttp2Configuration(configuration.getConfiguration("jetty.client.http2"));
        if (http2Configuration.isEnabled()) {
            // HTTP/2
            HTTP2Client http2Client = new HTTP2Client(connector);
            http2Client.setIdleTimeout(http2Configuration.getIdleTimeout());

            http2 = new ClientConnectionFactoryOverHTTP2.HTTP2(http2Client);
        }

        HttpClientTransportDynamic transport = null;
        JettyClientSslConfiguration jettyClientSslConfiguration = new JettyClientSslConfiguration(configuration.getConfiguration("jetty.client.ssl"));
        if (jettyClientSslConfiguration.isEnabled()) {
            SslContextFactory.Client sslClientContextFactory = clientSslContextFactoryProvider.get();
            connector.setSslContextFactory(sslClientContextFactory);
        }

        // Figure out correct transport dynamics
        if (http2 != null) {
            transport = new HttpClientTransportDynamic(connector, http2, http1);
        } else {
            transport = new HttpClientTransportDynamic(connector, http1);
        }

        client = new HttpClient(transport);
        QueuedThreadPool executor = new QueuedThreadPool();
        executor.setName(configuration.getString("instance.name").orElseThrow());
        client.setExecutor(executor);
        client.setScheduler(new ScheduledExecutorScheduler(configuration.getString("instance.name").orElseThrow() + "-scheduler", false));

        if (dnsLookup.get() != null) {
            client.setSocketAddressResolver(new DnsLookupSocketAddressResolver(dnsLookup.get()));
        } else {
            client.setSocketAddressResolver(new SocketAddressResolver.Async(client.getExecutor(), client.getScheduler(), client.getAddressResolutionTimeout()));
        }

        transport.setConnectionPoolFactory(destination ->
        {
            RoundRobinConnectionPool pool = new RoundRobinConnectionPool(destination, 10, destination);
//            pool.preCreateConnections(10);
            return pool;
        });

        client.start();

        logger.info("Started Jetty client");
    }

    public void preDestroy() {
        logger.info("Stopping Jetty client");
        try {
            client.stop();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public HttpClient provide() {
        return client;
    }
}
