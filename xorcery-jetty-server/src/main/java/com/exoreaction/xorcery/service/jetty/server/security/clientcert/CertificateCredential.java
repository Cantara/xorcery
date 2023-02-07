package com.exoreaction.xorcery.service.jetty.server.security.clientcert;

import io.jsonwebtoken.Claims;
import org.eclipse.jetty.util.security.Credential;

import java.security.cert.X509Certificate;

public class CertificateCredential extends Credential {

    private X509Certificate certificate;

    public CertificateCredential(X509Certificate certificate) {
        this.certificate = certificate;
    }

    @Override
    public boolean check(Object credentials) {
        return true;
    }

    public X509Certificate getCertificate() {
        return certificate;
    }

    @Override
    public String toString() {
        return certificate.getSubjectX500Principal().toString();
    }
}
