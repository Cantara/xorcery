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
package dev.xorcery.jetty.server.http2.providers;

import dev.xorcery.configuration.Configuration;
import dev.xorcery.jetty.server.JettyServerConfiguration;
import dev.xorcery.jetty.server.JettyServerSslConfiguration;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.glassfish.hk2.api.Factory;
import org.jvnet.hk2.annotations.Service;

@Service(name="jetty.server.http2")
public class Http2ConnectionFactoryFactory
    implements Factory<ConnectionFactory>
{
    private final HTTP2ServerConnectionFactory h2;

    @Inject
    public Http2ConnectionFactoryFactory(Configuration configuration) {
        JettyServerConfiguration jettyConfig = JettyServerConfiguration.get(configuration);
        JettyServerSslConfiguration sslConfig = JettyServerSslConfiguration.get(configuration);

        final HttpConfiguration sslHttpConfig = new HttpConfiguration();
        sslHttpConfig.setOutputBufferSize(jettyConfig.getOutputBufferSize());
        sslHttpConfig.setRequestHeaderSize(jettyConfig.getRequestHeaderSize());

        SecureRequestCustomizer customizer = new SecureRequestCustomizer();
        customizer.setSniRequired(sslConfig.isSniRequired());
        customizer.setSniHostCheck(sslConfig.isSniHostCheck());
        sslHttpConfig.addCustomizer(customizer);

        h2 = new HTTP2ServerConnectionFactory(sslHttpConfig);
    }

    @Override
    @Named("h2")
    @Singleton
    public ConnectionFactory provide() {
        return h2;
    }

    @Override
    public void dispose(ConnectionFactory instance) {

    }
}
