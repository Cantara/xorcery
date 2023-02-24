package com.exoreaction.xorcery.service.jetty.client;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.service.dns.client.DnsLookupService;
import com.exoreaction.xorcery.service.dns.client.api.DnsLookup;
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

import java.time.Duration;
import java.util.function.Supplier;

public class HttpClientFactory {

    private final Logger logger = LogManager.getLogger(getClass());
    private HttpClient client;

    public HttpClientFactory(Configuration configuration, Supplier<DnsLookupService> dnsLookup, Supplier<SslContextFactory.Client> clientSslContextFactoryProvider) throws Exception {

        Configuration clientConfig = configuration.getConfiguration("jetty.client");

        // Client setup
        ClientConnector connector = new ClientConnector();
        connector.setIdleTimeout(Duration.ofSeconds(clientConfig.getLong("idle_timeout").orElse(-1L)));

        // HTTP 1.1
        ClientConnectionFactory.Info http1 = HttpClientConnectionFactory.HTTP11;

        ClientConnectionFactoryOverHTTP2.HTTP2 http2 = null;

        if (clientConfig.getBoolean("http2.enabled").orElse(false)) {
            // HTTP/2
            HTTP2Client http2Client = new HTTP2Client(connector);
            http2Client.setIdleTimeout(clientConfig.getLong("idle_timeout").orElse(-1L));

            http2 = new ClientConnectionFactoryOverHTTP2.HTTP2(http2Client);
        }

        HttpClientTransportDynamic transport = null;
        if (clientConfig.getBoolean("ssl.enabled").orElse(false)) {
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
        executor.setName(configuration.getString("name").orElseThrow());
        client.setExecutor(executor);
        client.setScheduler(new ScheduledExecutorScheduler(configuration.getString("name").orElseThrow() + "-scheduler", false));

        if (dnsLookup.get() != null) {
            client.setSocketAddressResolver(new DnsLookupSocketAddressResolver(dnsLookup.get()));
        } else {
            client.setSocketAddressResolver(new SocketAddressResolver.Async(client.getExecutor(), client.getScheduler(), client.getAddressResolutionTimeout()));
        }

        transport.setConnectionPoolFactory(destination ->
        {
            RoundRobinConnectionPool pool = new RoundRobinConnectionPool(destination, 10, destination);
            pool.preCreateConnections(10);
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
