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
package com.exoreaction.xorcery.jetty.server;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.keystores.KeyStores;
import com.exoreaction.xorcery.keystores.KeyStoresConfiguration;
import com.exoreaction.xorcery.secrets.Secrets;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.PreDestroy;
import org.jvnet.hk2.annotations.Service;

import javax.security.auth.x500.X500Principal;
import java.io.InputStream;
import java.security.cert.CRL;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.stream.Collectors;

@Service(name = "jetty.server.ssl")
public class ServerSslContextFactoryFactory
        implements Factory<SslContextFactory.Server>, PreDestroy {

    private final Logger logger = LogManager.getLogger(getClass());

    private final SslContextFactory.Server factory;

    @Inject
    public ServerSslContextFactoryFactory(Configuration configuration, KeyStores keyStores, Secrets secrets) throws Exception {

        JettyServerSslConfiguration jettyServerSslConfiguration = new JettyServerSslConfiguration(configuration.getConfiguration("jetty.server.ssl"));
        KeyStoresConfiguration keyStoresConfiguration = new KeyStoresConfiguration(configuration.getConfiguration("keystores"));

        Collection<? extends CRL> crls = jettyServerSslConfiguration.getCRLs()
                .<Collection<? extends CRL>>map(url ->
                {
                    try (InputStream in = url.openStream()) {
                        return CertificateFactory.getInstance("X.509").generateCRLs(in);
                    } catch (Throwable throwable) {
                        LogManager.getLogger(ServerSslContextFactoryFactory.class).error("Could not load CRLs", throwable);
                        return Collections.emptyList();
                    }
                }).orElse(Collections.emptyList());

        if (!crls.isEmpty()) {
            logger.info("CRLs loaded:\n" + crls.stream()
                    .map(X509CRL.class::cast)
                    .map(X509CRL::getIssuerX500Principal)
                    .map(X500Principal::getName)
                    .collect(Collectors.joining("\n")));

            Date now = new Date();
            crls.stream().map(X509CRL.class::cast).forEach(crl ->
            {
                if (crl.getNextUpdate().before(now))
                    logger.warn("CRL has expired:" + crl.getIssuerX500Principal().getName());
            });
        }

        factory = new CustomSslContextFactoryServer(crls);
        jettyServerSslConfiguration.getKeyStoreName().ifPresentOrElse(name ->
        {
            factory.setKeyStore(keyStores.getKeyStore(name));
            factory.setKeyManagerPassword(keyStoresConfiguration.getKeyStoreConfiguration(name).getPassword().map(secrets::getSecretString).orElse(null));
            factory.setCertAlias(jettyServerSslConfiguration.getAlias());
        }, ()->
        {
            logger.warn("SSL enabled but no keystore specified");
        });
        jettyServerSslConfiguration.getTrustStoreName().ifPresent(name ->
        {
            factory.setTrustStore(keyStores.getKeyStore(name));
        });

        factory.setHostnameVerifier((hostName, session) -> true);
        factory.setTrustAll(jettyServerSslConfiguration.isTrustAll());
        factory.setNeedClientAuth(jettyServerSslConfiguration.isNeedClientAuth());
        factory.setWantClientAuth(jettyServerSslConfiguration.isWantClientAuth());
        factory.start();
    }

    @Override
    public void preDestroy() {
        try {
            factory.stop();
        } catch (Exception e) {
            LogManager.getLogger(getClass()).error("Could not stop SSL context factory", e);
        }
    }

    @Singleton
    @Named("jetty.server.ssl")
    @Override
    public SslContextFactory.Server provide() {
        return factory;
    }

    @Override
    public void dispose(SslContextFactory.Server instance) {
    }
}
