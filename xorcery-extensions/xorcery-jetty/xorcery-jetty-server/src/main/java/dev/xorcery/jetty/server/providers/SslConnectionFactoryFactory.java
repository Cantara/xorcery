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
package dev.xorcery.jetty.server.providers;

import dev.xorcery.configuration.Configuration;
import dev.xorcery.jetty.server.JettyServerConfiguration;
import dev.xorcery.jetty.server.JettyServerSslConfiguration;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.eclipse.jetty.server.*;
import org.glassfish.hk2.api.Factory;
import org.jvnet.hk2.annotations.Service;

@Service(name = "jetty.server.ssl")
public class SslConnectionFactoryFactory
        implements Factory<ConnectionFactory> {

    private final HttpConnectionFactory sslHttp11;

    @Inject
    public SslConnectionFactoryFactory(Configuration configuration) {
        JettyServerConfiguration jettyConfig = JettyServerConfiguration.get(configuration);
        JettyServerSslConfiguration sslConfig = JettyServerSslConfiguration.get(configuration);

        final HttpConfiguration sslHttpConfig = new HttpConfiguration();
        sslHttpConfig.setOutputBufferSize(jettyConfig.getOutputBufferSize());
        sslHttpConfig.setRequestHeaderSize(jettyConfig.getRequestHeaderSize());

        SecureRequestCustomizer customizer = new SecureRequestCustomizer();
        customizer.setSniRequired(sslConfig.isSniRequired());
        customizer.setSniHostCheck(sslConfig.isSniHostCheck());
        sslHttpConfig.addCustomizer(customizer);

        // Added for X-Forwarded-For support, from ALB
        sslHttpConfig.addCustomizer(new ForwardedRequestCustomizer());

        sslHttp11 = new HttpConnectionFactory(sslHttpConfig);
    }

    @Override
    @Named("ssl")
    @Singleton
    public ConnectionFactory provide() {
        return sslHttp11;
    }

    @Override
    public void dispose(ConnectionFactory instance) {

    }
}
