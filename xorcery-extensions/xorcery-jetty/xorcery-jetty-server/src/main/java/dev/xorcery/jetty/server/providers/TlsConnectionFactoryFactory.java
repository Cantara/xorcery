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

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.glassfish.hk2.api.Factory;
import org.jvnet.hk2.annotations.Optional;
import org.jvnet.hk2.annotations.Service;

@Service(name="jetty.server.ssl")
public class TlsConnectionFactoryFactory
    implements Factory<ConnectionFactory>
{
    private final SslConnectionFactory tls;

    @Inject
    public TlsConnectionFactoryFactory(
            SslContextFactory.Server sslContextFactory,
            @Optional @Named("alpn") ConnectionFactory alpn
    ) {
        tls = new SslConnectionFactory(sslContextFactory, alpn.getProtocol());
    }

    @Override
    @Named("tls")
    @Singleton
    public ConnectionFactory provide() {
        return tls;
    }

    @Override
    public void dispose(ConnectionFactory instance) {

    }
}
