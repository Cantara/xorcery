package com.exoreaction.xorcery.service.jetty.server;

import org.eclipse.jetty.util.security.CertificateValidator;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import java.security.KeyStore;
import java.security.cert.CRL;
import java.security.cert.X509Certificate;
import java.util.Collection;

public class CustomSslContextFactoryServer
    extends SslContextFactory.Server
{
    private Collection<? extends CRL> crls;

    public CustomSslContextFactoryServer(Collection<? extends CRL> crls) {
        this.crls = crls;
    }

    @Override
    public void validateCerts(X509Certificate[] certs) throws Exception {
        KeyStore trustStore = this.getTrustStore();
        CertificateValidator validator = new CertificateValidator(trustStore, crls);
        validator.validate(certs);
    }
}
