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
import dev.xorcery.keystores.KeyStoresConfiguration;
import dev.xorcery.secrets.Secrets;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.eclipse.jetty.util.ssl.SslContextFactory.Client.SniProvider.NON_DOMAIN_SNI_PROVIDER;

public class ClientSslContextFactory {
    private final List<SslContextFactory.Client> clients = new ArrayList<>();
    private final Logger logger;
    private final Optional<KeyStores> keyStores;
    private final Secrets secrets;
    private final KeyStoresConfiguration keyStoresConfiguration;

    public ClientSslContextFactory(Configuration configuration, Optional<KeyStores> keyStores, Secrets secrets, Logger logger) throws Exception {
        this.keyStores = keyStores;
        this.secrets = secrets;
        this.logger = logger;
        keyStoresConfiguration = KeyStoresConfiguration.get(configuration);
    }

    public SslContextFactory.Client newClient(JettyClientSslConfiguration jettyClientSslConfiguration) {
        SslContextFactory.Client client = new SslContextFactory.Client();

        // Client settings
        keyStores.ifPresentOrElse(ks ->
        {
            jettyClientSslConfiguration.getKeyStoreName().ifPresentOrElse(name ->
            {
                client.setKeyStore(Objects.requireNonNull(ks.getKeyStore(name), "No such KeyStore:" + name));
                client.setKeyManagerPassword(keyStoresConfiguration.getKeyStoreConfiguration(name).orElseThrow().getPassword().map(secrets::getSecretString).orElse(null));
                client.setCertAlias(jettyClientSslConfiguration.getAlias());
            }, () ->
            {
                logger.warn("SSL enabled but no keystore specified");
            });
            jettyClientSslConfiguration.getTrustStoreName().ifPresent(name ->
            {
                client.setTrustStore(ks.getKeyStore(name));
            });
        }, () ->
        {
            logger.warn("SSL enabled but no keystore specified");
        });

        // Server settings
        client.setEndpointIdentificationAlgorithm(jettyClientSslConfiguration.getEndpointIdentificationAlgorithm());
        client.setHostnameVerifier((hostName, session) -> true);
        client.setTrustAll(jettyClientSslConfiguration.isTrustAll());
        client.setSNIProvider(NON_DOMAIN_SNI_PROVIDER);

        try {
            client.start();
            clients.add(client);
            return client;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public void keyStoreUpdated(KeyStore updatedKeyStore) {
        try {
            for (SslContextFactory.Client client : clients) {
                client.reload(scf -> logger.info("Reloaded client keystore"));
            }
        } catch (Exception e) {
            logger.error("Error reloading client keystore", e);
        }
    }

    public void preDestroy() {
        for (SslContextFactory.Client client : clients) {
            try {
                client.stop();
            } catch (Exception e) {
                logger.error("Could not stop SSL context factory", e);
            }
        }
    }
}
