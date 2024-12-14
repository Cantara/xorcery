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
package dev.xorcery.jetty.client;

import dev.xorcery.configuration.Configuration;
import dev.xorcery.keystores.KeyStores;
import dev.xorcery.secrets.Secrets;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.hk2.api.messaging.MessageReceiver;
import org.glassfish.hk2.api.messaging.SubscribeTo;
import org.jvnet.hk2.annotations.Service;

import java.security.KeyStore;
import java.util.Optional;

@Service(name = "jetty.client.ssl")
@MessageReceiver(KeyStore.class)
public class ClientSslContextFactoryFactoryHK2 extends ClientSslContextFactoryFactory
        implements Factory<SslContextFactory.Client>, PreDestroy {

    @Inject
    public ClientSslContextFactoryFactoryHK2(Configuration configuration, Provider<KeyStores> keyStores, Secrets secrets) throws Exception {
        super(configuration, Optional.ofNullable(keyStores.get()), secrets);
    }

    @Override
    public void preDestroy() {
        super.preDestroy();
    }

    @Singleton
    @Override
    public SslContextFactory.Client provide() {
        return super.provide();
    }

    @Override
    public void dispose(SslContextFactory.Client instance) {
    }

    public void keyStoreUpdated(@SubscribeTo KeyStore updatedKeyStore) {
        super.keyStoreUpdated(updatedKeyStore);
    }
}
