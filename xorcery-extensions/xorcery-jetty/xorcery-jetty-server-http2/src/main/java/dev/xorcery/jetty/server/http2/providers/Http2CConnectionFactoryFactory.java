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
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.ForwardedRequestCustomizer;
import org.eclipse.jetty.server.HttpConfiguration;
import org.glassfish.hk2.api.Factory;
import org.jvnet.hk2.annotations.Service;

@Service(name="jetty.server.http2")
public class Http2CConnectionFactoryFactory
    implements Factory<ConnectionFactory>
{

    private final HTTP2CServerConnectionFactory h2c;

    @Inject
    public Http2CConnectionFactoryFactory(Configuration configuration) {
        JettyServerConfiguration jettyConfig = new JettyServerConfiguration(configuration.getConfiguration("jetty.server"));

        final HttpConfiguration httpConfig = new HttpConfiguration();
        httpConfig.setOutputBufferSize(jettyConfig.getOutputBufferSize());
        httpConfig.setRequestHeaderSize(jettyConfig.getRequestHeaderSize());

        // Added for X-Forwarded-For support, from ALB
        httpConfig.addCustomizer(new ForwardedRequestCustomizer());

        h2c = new HTTP2CServerConnectionFactory(httpConfig);
    }

    @Override
    @Named("h2c")
    @Singleton
    public ConnectionFactory provide() {
        return h2c;
    }

    @Override
    public void dispose(ConnectionFactory instance) {

    }
}
