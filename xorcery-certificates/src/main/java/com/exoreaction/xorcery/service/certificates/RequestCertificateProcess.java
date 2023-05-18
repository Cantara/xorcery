package com.exoreaction.xorcery.service.certificates;

import com.exoreaction.xorcery.concurrent.CompletableFutures;
import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.process.Process;
import com.exoreaction.xorcery.service.certificates.spi.CertificatesProvider;
import com.exoreaction.xorcery.service.keystores.KeyStores;
import jakarta.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.glassfish.hk2.api.IterableProvider;

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
            CertificatesConfiguration certificatesConfig = () -> configuration.getConfiguration("certificates");
            return new RequestCertificateProcess(csr, new CompletableFuture<>(),
                    keyStores.getKeyStore("keystore"), keyStores,
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
                certs.whenComplete(CompletableFutures.transfer(result));
            } else {
                logger.warn("No certificate providers found");
            }
        } catch (Throwable e) {
            logger.warn("Could not create CSR", e);
            result.completeExceptionally(e);
        }
    }
}
