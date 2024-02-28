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
package com.exoreaction.xorcery.certificates;

import com.exoreaction.xorcery.certificates.spi.CertificatesProvider;
import com.exoreaction.xorcery.concurrent.CompletableFutures;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.process.Process;
import com.exoreaction.xorcery.keystores.KeyStores;
import jakarta.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.glassfish.hk2.api.IterableProvider;

import java.net.ConnectException;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public record RequestCertificateProcess(PKCS10CertificationRequest csr,
                                        CompletableFuture<List<X509Certificate>> result,
                                        KeyStore keyStore,
                                        KeyStores keyStores,
                                        CertificatesConfiguration certificatesConfiguration,
                                        IterableProvider<CertificatesProvider> providers,
                                        Logger logger)
        implements Process<List<X509Certificate>> {

    public record Factory(KeyStores keyStores,
                          IterableProvider<CertificatesProvider> providers,
                          Configuration configuration) {
        @Inject
        public Factory {
        }

        public RequestCertificateProcess create(PKCS10CertificationRequest csr) {
            CertificatesConfiguration certificatesConfig = CertificatesConfiguration.get(configuration);
            return new RequestCertificateProcess(csr, new CompletableFuture<>(),
                    keyStores.getKeyStore(certificatesConfig.getCertificateStoreName()), keyStores,
                    certificatesConfig, providers, LogManager.getLogger(RequestCertificateProcess.class));
        }
    }

    @Override
    public void start() {
        try {
            CompletionStage<List<X509Certificate>> certs = null;
            for (CertificatesProvider provider : providers) {
                if (certs == null) {
                    certs = provider.requestCertificates(csr);
                } else {
                    certs = certs.thenCombine(provider.requestCertificates(csr), (l1, l2) ->
                    {
                        var l3 = new ArrayList<>(l1);
                        l3.addAll(l2);
                        return l3;
                    });
                }
            }
            if (certs != null) {
                certs.whenComplete(this::complete);
            } else {
                logger.warn("No certificate providers found");
                result.completeExceptionally(new IllegalStateException("No certificate providers found"));
            }
        } catch (Throwable e) {
            logger.warn("Could not create CSR", e);
            result.completeExceptionally(e);
        }
    }

    @Override
    public boolean isRetryable(Throwable t) {
        return t instanceof ConnectException;
    }

    @Override
    public void error(Throwable throwable) {
        logger.error("Could not renew certificate", throwable);
    }

    @Override
    public void debug(String message) {
        logger.debug(message);
    }
}
