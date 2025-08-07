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
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.hk2.api.messaging.MessageReceiver;
import org.glassfish.hk2.api.messaging.SubscribeTo;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

import java.security.KeyStore;
import java.util.Optional;

@Service
@MessageReceiver(KeyStore.class)
@ContractsProvided(ClientSslContextFactory.class)
public class ClientSslContextFactoryHK2 extends ClientSslContextFactory
        implements PreDestroy {

    @Inject
    public ClientSslContextFactoryHK2(Configuration configuration, Provider<KeyStores> keyStores, Secrets secrets, Logger logger) throws Exception {
        super(configuration, Optional.ofNullable(keyStores.get()), secrets, logger);
    }

    @Override
    public void preDestroy() {
        super.preDestroy();
    }

    public void keyStoreUpdated(@SubscribeTo KeyStore updatedKeyStore) {
        super.keyStoreUpdated(updatedKeyStore);
    }
}
