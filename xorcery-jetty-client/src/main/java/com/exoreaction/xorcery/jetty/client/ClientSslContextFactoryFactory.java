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
package com.exoreaction.xorcery.jetty.client;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.keystores.KeyStores;
import com.exoreaction.xorcery.keystores.KeyStoresConfiguration;
import com.exoreaction.xorcery.secrets.Secrets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import java.security.KeyStore;
import java.util.Objects;
import java.util.Optional;

import static org.eclipse.jetty.util.ssl.SslContextFactory.Client.SniProvider.NON_DOMAIN_SNI_PROVIDER;

public class ClientSslContextFactoryFactory {
    private final SslContextFactory.Client factory;
    private final Logger logger = LogManager.getLogger(getClass());

    public ClientSslContextFactoryFactory(Configuration configuration, Optional<KeyStores> keyStores, Secrets secrets) throws Exception {
        factory = new SslContextFactory.Client();

        JettyClientSslConfiguration jettyClientSslConfiguration = new JettyClientSslConfiguration(configuration.getConfiguration("jetty.client.ssl"));
        KeyStoresConfiguration keyStoresConfiguration = new KeyStoresConfiguration(configuration.getConfiguration("keystores"));

        // Client settings
        keyStores.ifPresentOrElse(ks ->
        {
            jettyClientSslConfiguration.getKeyStoreName().ifPresentOrElse(name ->
            {
                factory.setKeyStore(Objects.requireNonNull(ks.getKeyStore(name), "No such KeyStore:"+name));
                factory.setKeyManagerPassword(keyStoresConfiguration.getKeyStoreConfiguration(name).getPassword().map(secrets::getSecretString).orElse(null));
                factory.setCertAlias(jettyClientSslConfiguration.getAlias());
            }, ()->
            {
                logger.warn("SSL enabled but no keystore specified");
            });
            jettyClientSslConfiguration.getTrustStoreName().ifPresent(name ->
            {
                factory.setTrustStore(ks.getKeyStore(name));
            });
        },()->
        {
            logger.warn("SSL enabled but no keystore specified");
        });

        // Server settings
        factory.setEndpointIdentificationAlgorithm(jettyClientSslConfiguration.getEndpointIdentificationAlgorithm());
        factory.setHostnameVerifier((hostName, session) -> true);
        factory.setTrustAll(jettyClientSslConfiguration.isTrustAll());
        factory.setSNIProvider(NON_DOMAIN_SNI_PROVIDER);

        factory.start();
    }

    public void preDestroy() {
        try {
            factory.stop();
        } catch (Exception e) {
            LogManager.getLogger(getClass()).error("Could not stop SSL context factory", e);
        }
    }

    public SslContextFactory.Client provide() {
        return factory;
    }

    public void keyStoreUpdated(KeyStore updatedKeyStore) {
        try {
            factory.reload(scf ->
                    LogManager.getLogger(getClass()).info("Reloaded client keystore"));
        } catch (Exception e) {
            LogManager.getLogger(getClass()).error("Error reloading client keystore", e);
        }
    }
}
