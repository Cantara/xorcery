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
package com.exoreaction.xorcery.jetty.server;

import com.exoreaction.xorcery.configuration.Configuration;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory;
import org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory;
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.Immediate;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

@Service(name = "jetty.server")
@RunLevel(4)
public class JettyServerFactory
        implements Factory<Server> {
    private final Server server;

    @Inject
    public JettyServerFactory(Configuration configuration, Provider<SslContextFactory.Server> sslContextFactoryProvider, Logger logger) {

        logger.info("Jetty factory");
        JettyServerConfiguration jettyConfig = new JettyServerConfiguration(configuration.getConfiguration("jetty.server"));
        JettyServerHttp2Configuration jettyHttp2Config = new JettyServerHttp2Configuration(configuration.getConfiguration("jetty.server.http2"));
        JettyServerSslConfiguration jettyServerSslConfiguration = new JettyServerSslConfiguration(configuration.getConfiguration("jetty.server.ssl"));

        int httpPort = jettyConfig.getHttpPort();
        int httpsPort = jettyServerSslConfiguration.getPort();

        // Setup thread pool
        JettyConnectorThreadPool jettyConnectorThreadPool = new JettyConnectorThreadPool();
        jettyConnectorThreadPool.setName("jetty-http-server-");
        jettyConnectorThreadPool.setMinThreads(jettyConfig.getMinThreads());
        jettyConnectorThreadPool.setMaxThreads(jettyConfig.getMaxThreads());

        // Create server
        server = new Server(jettyConnectorThreadPool);

        // Setup connector
        final HttpConfiguration httpConfig = new HttpConfiguration();
        httpConfig.setOutputBufferSize(jettyConfig.getOutputBufferSize());
        httpConfig.setRequestHeaderSize(jettyConfig.getRequestHeaderSize());

        // Added for X-Forwarded-For support, from ALB
        httpConfig.addCustomizer(new ForwardedRequestCustomizer());

        // Setup protocols

        // Clear-text protocols
        if (jettyConfig.isHttpEnabled()) {
            HttpConnectionFactory http11 = new HttpConnectionFactory(httpConfig);
            ServerConnector httpConnector;
            if (jettyHttp2Config.isEnabled()) {
                // The ConnectionFactory for clear-text HTTP/2.
                HTTP2CServerConnectionFactory h2c = new HTTP2CServerConnectionFactory(httpConfig);

                // Create and configure the HTTP 1.1/2 connector
                httpConnector = new ServerConnector(server, http11, h2c);
            } else {
                // Create and configure the HTTP 1.1 connector
                httpConnector = new ServerConnector(server, http11);
            }
            httpConnector.setIdleTimeout(jettyConfig.getIdleTimeout().toSeconds());
            httpConnector.setPort(httpPort);
            server.addConnector(httpConnector);
        }

        // SSL
        if (jettyServerSslConfiguration.isEnabled()) {

            SslContextFactory.Server sslContextFactory = sslContextFactoryProvider.get();
            server.addBean(sslContextFactory);

            final HttpConfiguration sslHttpConfig = new HttpConfiguration();
            sslHttpConfig.setOutputBufferSize(jettyConfig.getOutputBufferSize());
            sslHttpConfig.setRequestHeaderSize(jettyConfig.getRequestHeaderSize());

            SecureRequestCustomizer customizer = new SecureRequestCustomizer();
            customizer.setSniRequired(jettyServerSslConfiguration.isSniRequired());
            customizer.setSniHostCheck(jettyServerSslConfiguration.isSniHostCheck());
            sslHttpConfig.addCustomizer(customizer);

            // Added for X-Forwarded-For support, from ALB
            sslHttpConfig.addCustomizer(new ForwardedRequestCustomizer());

            HttpConnectionFactory sslHttp11 = new HttpConnectionFactory(sslHttpConfig);

            // The ALPN ConnectionFactory.
            ALPNServerConnectionFactory alpn = new ALPNServerConnectionFactory();
            // The default protocol to use in case there is no negotiation.
            alpn.setDefaultProtocol(sslHttp11.getProtocol());

            SslConnectionFactory tls = new SslConnectionFactory(sslContextFactory, alpn.getProtocol());

            // Create and configure the secure HTTP 1.1/2 connector, with ALPN negotiation
            ServerConnector httpsConnector;
            if (jettyHttp2Config.isEnabled()) {
                HTTP2ServerConnectionFactory h2 = new HTTP2ServerConnectionFactory(sslHttpConfig);
                httpsConnector = new ServerConnector(server, tls, alpn, h2, sslHttp11);
            } else {
                httpsConnector = new ServerConnector(server, tls, alpn, sslHttp11);
            }
            httpsConnector.setIdleTimeout(jettyConfig.getIdleTimeout().toSeconds());
            httpsConnector.setPort(httpsPort);
            server.addConnector(httpsConnector);
        }

        Slf4jRequestLogWriter requestLog = new Slf4jRequestLogWriter();
        requestLog.setLoggerName("jetty");
        server.setRequestLog(
                new CustomRequestLog(requestLog, "%{client}a - %u %t \"%r\" %s %O \"%{Referer}i\" \"%{User-Agent}i\""));
    }

    @Override
    @Singleton
    @Named("jetty.server")
    public Server provide() {
        return server;
    }

    @Override
    public void dispose(Server instance) {
    }
}
