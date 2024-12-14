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
package dev.xorcery.jetty.server;

import dev.xorcery.configuration.Configuration;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.IterableProvider;
import org.jvnet.hk2.annotations.Service;

@Service(name = "jetty.server")
public class JettyServerFactory
        implements Factory<Server> {
    private final Server server;

    @Inject
    public JettyServerFactory(
            Configuration configuration,
            Provider<SslContextFactory.Server> sslContextFactoryProvider,
            IterableProvider<ConnectionFactory> connectionFactories,
            Logger logger) {

        JettyServerConfiguration jettyConfig = JettyServerConfiguration.get(configuration);
        JettyServerSslConfiguration jettyServerSslConfiguration = JettyServerSslConfiguration.get(configuration);

        int httpsPort = jettyServerSslConfiguration.getPort();

        // Setup thread pool
        JettyConnectorThreadPool jettyConnectorThreadPool = new JettyConnectorThreadPool();
        jettyConnectorThreadPool.setName("jetty-http-server");
        jettyConnectorThreadPool.setMinThreads(jettyConfig.getMinThreads());
        jettyConnectorThreadPool.setMaxThreads(jettyConfig.getMaxThreads());

        // Create server
        server = new Server(jettyConnectorThreadPool);

        // Register mime type extension mappings
        jettyConfig.getMediaTypes().ifPresent(types -> types.forEach((ext,type)->server.getMimeTypes().addMimeMapping(ext, type)));

        // Setup protocols

        // Clear-text protocols
        if (jettyConfig.isHttpEnabled()) {
            // Setup connector
            ConnectionFactory http11 = connectionFactories.named("http11").get();
            ServerConnector httpConnector;
            if (connectionFactories.named("h2c").getSize()>0) {
                // The ConnectionFactory for clear-text HTTP/2.
                ConnectionFactory h2c = connectionFactories.named("h2c").get();

                // Create and configure the HTTP 1.1/2 connector
                httpConnector = new ServerConnector(server, http11, h2c);
            } else {
                // Create and configure the HTTP 1.1 connector
                httpConnector = new ServerConnector(server, http11);
            }
            httpConnector.setIdleTimeout(jettyConfig.getIdleTimeout().toSeconds());
            httpConnector.setPort(jettyConfig.getHttpPort());
            server.addConnector(httpConnector);
        }

        // SSL
        if (jettyServerSslConfiguration.isEnabled()) {

            SslContextFactory.Server sslContextFactory = sslContextFactoryProvider.get();
            server.addBean(sslContextFactory);
            ConnectionFactory sslHttp11 = connectionFactories.named("ssl").get();
            ConnectionFactory alpn = connectionFactories.named("alpn").get();
            ConnectionFactory tls = connectionFactories.named("tls").get();

            // Create and configure the secure HTTP 1.1/2 connector, with ALPN negotiation
            ServerConnector httpsConnector;
            if (connectionFactories.named("h2").getSize()>0)
            {
                ConnectionFactory h2 = connectionFactories.named("h2").get();
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

    @Singleton
    @Override
    public Server provide() {
        return server;
    }

    @Override
    public void dispose(Server instance) {
    }
}
