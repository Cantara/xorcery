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
import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory;
import org.eclipse.jetty.server.ConnectionFactory;
import org.glassfish.hk2.api.Factory;
import org.jvnet.hk2.annotations.Service;

@Service(name="jetty.server.ssl")
public class ALPNServerConnectionFactoryFactory
    implements Factory<ConnectionFactory>
{
    private final ALPNServerConnectionFactory alpn;

    @Inject
    public ALPNServerConnectionFactoryFactory(@Named("ssl") ConnectionFactory sslHttp11) {
        alpn = new ALPNServerConnectionFactory();
        // The default protocol to use in case there is no negotiation.
        alpn.setDefaultProtocol(sslHttp11.getProtocol());
    }

    @Named("alpn")
    @Singleton
    @Override
    public ConnectionFactory provide() {
        return alpn;
    }

    @Override
    public void dispose(ConnectionFactory instance) {

    }
}
