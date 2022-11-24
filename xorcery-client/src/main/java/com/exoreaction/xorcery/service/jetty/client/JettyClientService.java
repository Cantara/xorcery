package com.exoreaction.xorcery.service.jetty.client;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.service.dns.client.DnsLookupService;
import com.exoreaction.xorcery.service.dns.client.api.DnsLookup;
import com.exoreaction.xorcery.service.jetty.client.dns.ConfigurationSocketAddressResolver;
import com.exoreaction.xorcery.service.jetty.client.dns.DnsLookupSocketAddressResolver;
import com.exoreaction.xorcery.service.jetty.client.dns.SRVSocketAddressResolver;
import com.exoreaction.xorcery.util.Resources;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.RoundRobinConnectionPool;
import org.eclipse.jetty.client.dynamic.HttpClientTransportDynamic;
import org.eclipse.jetty.client.http.HttpClientConnectionFactory;
import org.eclipse.jetty.http2.client.HTTP2Client;
import org.eclipse.jetty.http2.client.http.ClientConnectionFactoryOverHTTP2;
import org.eclipse.jetty.io.ClientConnectionFactory;
import org.eclipse.jetty.io.ClientConnector;
import org.eclipse.jetty.util.SocketAddressResolver;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ScheduledExecutorScheduler;
import org.glassfish.hk2.api.Factory;
import org.jvnet.hk2.annotations.Service;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.time.Duration;

import static org.eclipse.jetty.util.ssl.SslContextFactory.Client.SniProvider.NON_DOMAIN_SNI_PROVIDER;

@Service(name = "client")
public class JettyClientService
        implements Factory<HttpClient> {

    private HttpClient client;

    @Inject
    public JettyClientService(Configuration configuration, Provider<DnsLookup> dnsLookup) throws Exception {
        // Client setup
        ClientConnector connector = new ClientConnector();
        connector.setIdleTimeout(Duration.ofSeconds(configuration.getLong("client.idle_timeout").orElse(-1L)));

        // HTTP 1.1
        ClientConnectionFactory.Info http1 = HttpClientConnectionFactory.HTTP11;

        ClientConnectionFactoryOverHTTP2.HTTP2 http2 = null;

        if (configuration.getBoolean("client.http2.enabled").orElse(false)) {
            // HTTP/2
            HTTP2Client http2Client = new HTTP2Client(connector);
            http2Client.setIdleTimeout(configuration.getLong("client.idle_timeout").orElse(-1L));

            http2 = new ClientConnectionFactoryOverHTTP2.HTTP2(http2Client);
        }

        HttpClientTransportDynamic transport = null;
        if (configuration.getBoolean("client.ssl.enabled").orElse(false)) {

            SslContextFactory.Client sslClientContextFactory = new SslContextFactory.Client() {
                @Override
                protected KeyStore loadTrustStore(Resource resource) throws Exception {
                    KeyStore keyStore = super.loadTrustStore(resource);
                    addDefaultRootCaCertificates(keyStore);
                    return keyStore;
                }
            };
            sslClientContextFactory.setKeyStoreType(configuration.getString("client.ssl.keystore.type").orElse("PKCS12"));
            sslClientContextFactory.setKeyStorePath(configuration.getResourcePath("client.ssl.keystore.path")
                    .orElseGet(() -> Resources.getResource("META-INF/keystore.p12").orElseThrow().getPath()));
            sslClientContextFactory.setKeyStorePassword(configuration.getString("client.ssl.keystore.password").orElse("password"));

//                        sslClientContextFactory.setTrustStoreType(configuration.getString("client.ssl.truststore.type").orElse("PKCS12"));
            sslClientContextFactory.setTrustStorePath(configuration.getResourcePath("client.ssl.truststore.path")
                    .orElseGet(() -> Resources.getResource("META-INF/truststore.jks").orElseThrow().getPath()));
            sslClientContextFactory.setTrustStorePassword(configuration.getString("client.ssl.truststore.password").orElse("password"));

            sslClientContextFactory.setEndpointIdentificationAlgorithm("HTTPS");
            sslClientContextFactory.setHostnameVerifier((hostName, session) -> true);
            sslClientContextFactory.setTrustAll(configuration.getBoolean("client.ssl.trustall").orElse(false));
            sslClientContextFactory.setSNIProvider(NON_DOMAIN_SNI_PROVIDER);


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

        if (configuration.getBoolean("dns.enabled").orElse(false) && dnsLookup.get() != null) {
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
    }

    @Override
    @Singleton
    public HttpClient provide() {
        return client;
    }

    @Override
    public void dispose(HttpClient instance) {
        try {
            client.stop();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected void addDefaultRootCaCertificates(KeyStore trustStore) throws GeneralSecurityException {
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        // Loads default Root CA certificates (generally, from JAVA_HOME/lib/cacerts)
        trustManagerFactory.init((KeyStore) null);
        for (TrustManager trustManager : trustManagerFactory.getTrustManagers()) {
            if (trustManager instanceof X509TrustManager) {
                for (X509Certificate acceptedIssuer : ((X509TrustManager) trustManager).getAcceptedIssuers()) {
                    trustStore.setCertificateEntry(acceptedIssuer.getSubjectDN().getName(), acceptedIssuer);
                }
            }
        }
    }

}
