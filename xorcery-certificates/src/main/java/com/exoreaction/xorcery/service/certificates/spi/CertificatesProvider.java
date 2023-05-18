package com.exoreaction.xorcery.service.certificates.spi;

import org.bouncycastle.pkcs.PKCS10CertificationRequest;

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.concurrent.CompletionStage;

public interface CertificatesProvider {
    CompletionStage<List<X509Certificate>> requestCertificates(PKCS10CertificationRequest csr);
}
