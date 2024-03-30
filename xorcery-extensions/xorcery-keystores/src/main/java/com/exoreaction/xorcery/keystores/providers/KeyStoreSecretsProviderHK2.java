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
package com.exoreaction.xorcery.keystores.providers;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.keystores.KeyStores;
import com.exoreaction.xorcery.secrets.Secrets;
import com.exoreaction.xorcery.secrets.spi.SecretsProvider;
import jakarta.inject.Inject;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

@Service(name = "keystore", metadata = "enabled=secrets.keystore.enabled")
@ContractsProvided(SecretsProvider.class)
public class KeyStoreSecretsProviderHK2
        extends KeyStoreSecretsProvider {
    @Inject
    public KeyStoreSecretsProviderHK2(Configuration configuration, KeyStores keyStores, Secrets secrets) throws NoSuchAlgorithmException, IOException {
        super(configuration, keyStores, secrets);
    }
}
