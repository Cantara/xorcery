package com.exoreaction.xorcery.service.jetty.server.security.clientcert;

import org.eclipse.jetty.security.UserPrincipal;

import javax.security.auth.x500.X500Principal;
import java.security.cert.X509Certificate;

public class ClientCertUserPrincipal
        extends UserPrincipal {
    private final X500Principal x500Principal;

    public ClientCertUserPrincipal(X500Principal x500Principal, CertificateCredential credential) {
        super(x500Principal.getName(), credential);
        this.x500Principal = x500Principal;
    }

    public X500Principal getX500Principal() {
        return x500Principal;
    }

    public X509Certificate getCertificate() {
        return ((CertificateCredential) _credential).getCertificate();
    }
}
