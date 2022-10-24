package com.exoreaction.xorcery.service.jetty.server;

import com.exoreaction.xorcery.configuration.model.Configuration;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory;
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.glassfish.hk2.api.*;
import org.jvnet.hk2.annotations.Service;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

@Service
public class JettyServerService
    implements Factory<Server>
{
    private final Server server;

    @Inject
    public JettyServerService(Configuration configuration) {

            Configuration jettyConfig = configuration.getConfiguration("server");

            int httpPort = jettyConfig.getInteger("http.port").orElse(8889);
            int httpsPort = jettyConfig.getInteger("ssl.port").orElse(8443);

            // Setup thread pool
            JettyConnectorThreadPool jettyConnectorThreadPool = new JettyConnectorThreadPool();
            jettyConnectorThreadPool.setName("jetty-http-server-");
            jettyConnectorThreadPool.setMinThreads(10);
            jettyConnectorThreadPool.setMaxThreads(150);

            // Create server
            server = new Server(jettyConnectorThreadPool);
            server.setStopAtShutdown(true);

            // Setup connector
            final HttpConfiguration httpConfig = new HttpConfiguration();
            httpConfig.setOutputBufferSize(32768);
            httpConfig.setRequestHeaderSize(1024 * 16);

            SecureRequestCustomizer customizer = new SecureRequestCustomizer();
            customizer.setSniRequired(configuration.getBoolean("server.ssl.snirequired").orElse(true));
            customizer.setSniHostCheck(configuration.getBoolean("server.ssl.snihostcheck").orElse(true));
            httpConfig.addCustomizer(customizer);

            // Added for X-Forwarded-For support, from ALB
            httpConfig.addCustomizer(new ForwardedRequestCustomizer());

            // Setup protocols
            HttpConnectionFactory http11 = new HttpConnectionFactory(httpConfig);

            if (configuration.getBoolean("server.http2.enabled").orElse(false)) {
                // The ConnectionFactory for clear-text HTTP/2.
                HTTP2CServerConnectionFactory h2c = new HTTP2CServerConnectionFactory(httpConfig);

                // Create and configure the HTTP 1.1/2 connector
                final ServerConnector http = new ServerConnector(server, http11, h2c);
                http.setPort(httpPort);
                server.addConnector(http);
            } else {
                // Create and configure the HTTP 1.1 connector
                final ServerConnector http = new ServerConnector(server, http11);
                http.setPort(httpPort);
                server.addConnector(http);
            }

            // Configure the SslContextFactory with the keyStore information.
            SslContextFactory.Server sslContextFactory = configuration.getBoolean("server.ssl.enabled").orElse(false) ?
                    new SslContextFactory.Server() {
                        @Override
                        protected KeyStore loadTrustStore(Resource resource) throws Exception {
                            KeyStore keyStore = super.loadTrustStore(resource);
                            addDefaultRootCaCertificates(keyStore);
                            return keyStore;
                        }
                    } : null;

            if (configuration.getBoolean("server.ssl.enabled").orElse(false)) {
                // The ALPN ConnectionFactory.
/*
            ALPNServerConnectionFactory alpn = new ALPNServerConnectionFactory();
            // The default protocol to use in case there is no negotiation.
            alpn.setDefaultProtocol(http11.getProtocol());
*/

                sslContextFactory.setKeyStoreType(configuration.getString("server.ssl.keystore.type").orElse("PKCS12"));
                sslContextFactory.setKeyStorePath(configuration.getString("server.ssl.keystore.path")
                        .orElseGet(() -> ClassLoader.getSystemResource("keystore.p12").toExternalForm()));
                sslContextFactory.setKeyStorePassword(configuration.getString("server.ssl.keystore.password").orElse("password"));

//            sslContextFactory.setTrustStoreType(configuration.getString("server.ssl.truststore.type").orElse("PKCS12"));
                sslContextFactory.setTrustStorePath(configuration.getString("server.ssl.truststore.path")
                        .orElseGet(() -> ClassLoader.getSystemResource("keystore.p12").toExternalForm()));
                sslContextFactory.setTrustStorePassword(configuration.getString("server.ssl.truststore.password").orElse("password"));
                sslContextFactory.setHostnameVerifier((hostName, session) -> true);
                sslContextFactory.setTrustAll(configuration.getBoolean("server.ssl.trustall").orElse(false));
                sslContextFactory.setWantClientAuth(configuration.getBoolean("server.ssl.wantclientauth").orElse(false));

//            SslConnectionFactory tls = new SslConnectionFactory(sslContextFactory, alpn.getProtocol());
                SslConnectionFactory tls = new SslConnectionFactory(sslContextFactory, http11.getProtocol());

                // Create and configure the secure HTTP 1.1/2 connector
                ServerConnector https;
                if (configuration.getBoolean("server.http2.enabled").orElse(false)) {
                    HTTP2ServerConnectionFactory h2 = new HTTP2ServerConnectionFactory(httpConfig);
                    https = new ServerConnector(server, tls, h2, http11);
                } else {
                    https = new ServerConnector(server, tls, http11);
                }
                https.setIdleTimeout(jettyConfig.getLong("idle_timeout").orElse(-1L));
                https.setPort(httpsPort);
                server.addConnector(https);
            }

            Slf4jRequestLogWriter requestLog = new Slf4jRequestLogWriter();
            requestLog.setLoggerName("jetty");
            server.setRequestLog(
                    new CustomRequestLog(requestLog, "%{client}a - %u %t \"%r\" %s %O \"%{Referer}i\" \"%{User-Agent}i\""));
    }

    @Override
    @Singleton
    @Named("server")
    public Server provide() {
        return server;
    }

    @Override
    public void dispose(Server instance) {
        try {
            server.stop();
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
