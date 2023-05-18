package com.exoreaction.xorcery.service.certificates.server;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.configuration.model.InstanceConfiguration;
import com.exoreaction.xorcery.server.api.ServiceResourceObjects;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import com.exoreaction.xorcery.service.certificates.spi.CertificatesProvider;
import com.exoreaction.xorcery.service.keystores.KeyStores;
import jakarta.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.operator.jcajce.JcaContentVerifierProviderBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.glassfish.hk2.api.IterableProvider;
import org.glassfish.hk2.api.ServiceHandle;
import org.jvnet.hk2.annotations.Service;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Service(name = "certificates.server")
public class ServerCertificatesProvider {

    private final Logger logger = LogManager.getLogger(getClass());
    private final KeyStore intermediateCaTrustStore;
    private final List<CertificatesProvider> certificatesProviders = new ArrayList<>();
    private final CertificatesServerConfiguration config;

    @Inject
    public ServerCertificatesProvider(KeyStores keyStores,
                                      Configuration configuration,
                                      IterableProvider<CertificatesProvider> certificatesManagementProviders
    ) {
        this.config = new CertificatesServerConfiguration(configuration.getConfiguration("certificates.server"));
        intermediateCaTrustStore = keyStores.getKeyStore("truststore");
        for (ServiceHandle<CertificatesProvider> certificatesProviderServiceHandle : certificatesManagementProviders.handleIterator()) {
            if (!certificatesProviderServiceHandle.getActiveDescriptor().getImplementation().contains("ClientCertificatesProvider"))
                certificatesProviders.add(certificatesProviderServiceHandle.getService());
        }
    }

    public CompletionStage<String> requestCertificate(String csrPemEncoded) {

        return parseCsr(csrPemEncoded)
                .thenCompose(this::validateCsr)
                .thenCompose(this::getCertificates)
                .thenCompose(this::toPem);
    }

    private CompletionStage<PKCS10CertificationRequest> parseCsr(String csrPemEncoded) {
        try {
            return CompletableFuture.completedStage((PKCS10CertificationRequest) new PEMParser(new StringReader(csrPemEncoded)).readObject());
        } catch (IOException e) {
            return CompletableFuture.failedStage(e);
        }
    }

    private CompletionStage<PKCS10CertificationRequest> validateCsr(PKCS10CertificationRequest csr) {
        try {
            switch (config.getAuthorizationType())
            {
            case none -> {
                    // All requests are accepted
                }
                case provisioning -> {
                // Check if CSR was signed using the known provisioning key
                    X509Certificate rootCert = (X509Certificate) intermediateCaTrustStore.getCertificate("root");
                    boolean isValid = csr.isSignatureValid(new JcaContentVerifierProviderBuilder().build(rootCert));
                    if (!isValid)
                        return CompletableFuture.failedStage(new IllegalArgumentException("CSR not signed by provisioning CA"));
                }
                case dns -> {
                    // TODO Check if IP address matches DNS name through lookup
                }
                case ip -> {
                    // TODO Check if IP address is in a particular range
                }
            }

            return CompletableFuture.completedStage(csr);
        } catch (Throwable e) {
            return CompletableFuture.failedStage(e);
        }
    }

    private CompletionStage<List<X509Certificate>> getCertificates(PKCS10CertificationRequest csr) {

        CompletionStage<List<X509Certificate>> certs = null;
        for (CertificatesProvider provider : certificatesProviders) {
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
            return certs;
        } else {
            logger.warn("No certificate providers found");
            return CompletableFuture.failedStage(new IllegalArgumentException("No certificate providers found"));
        }
    }

    private CompletionStage<String> toPem(List<X509Certificate> certificates) {
        try {
            StringBuilder pem = new StringBuilder();
            for (X509Certificate certificate : certificates) {
                pem.append(toPEM(certificate));
            }
            return CompletableFuture.completedStage(pem.toString());
        } catch (Throwable e) {
            return CompletableFuture.failedStage(e);
        }
    }

    private String toPEM(X509Certificate certificate) throws CertificateException, IOException {
        StringWriter stringWriter = new StringWriter();
        PemWriter pWrt = new PemWriter(stringWriter);
        pWrt.writeObject(new PemObject(PEMParser.TYPE_CERTIFICATE, certificate.getEncoded()));
        pWrt.close();
        return stringWriter.toString();
    }
}
