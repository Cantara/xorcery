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
package com.exoreaction.xorcery.service.keystores;

import com.exoreaction.xorcery.configuration.model.Configuration;
import jakarta.inject.Inject;
import org.glassfish.hk2.api.messaging.Topic;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

@Service(name = "keystores")
@ContractsProvided({com.exoreaction.xorcery.service.keystores.KeyStores.class})
public class KeyStoresHK2 extends com.exoreaction.xorcery.service.keystores.KeyStores {

    private final Topic<KeyStore> keyStoreTopic;

    @Inject
    public KeyStoresHK2(Configuration configuration, Topic<KeyStore> keyStoreTopic) throws NoSuchAlgorithmException, NoSuchProviderException {
        super(configuration);
        this.keyStoreTopic = keyStoreTopic;
    }

    @Override
    protected void publish(KeyStore keyStore) {
        keyStoreTopic.publish(keyStore);
    }
}
