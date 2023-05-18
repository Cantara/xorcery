package com.exoreaction.xorcery.service.certificates.ca.resources;

import com.exoreaction.xorcery.service.certificates.ca.IntermediateCACertificatesProvider;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import org.bouncycastle.operator.OperatorCreationException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStoreException;
import java.security.cert.CertificateEncodingException;

@Path("api/ca")
@Singleton
public class CertificateAuthorityResource {

    private final IntermediateCACertificatesProvider intermediateCA;

    @Inject
    public CertificateAuthorityResource(IntermediateCACertificatesProvider intermediateCA) {
        this.intermediateCA = intermediateCA;
    }

    @GET
    @Path("rootca.cer")
    @Produces("application/x-x509-ca-cert")
    public byte[] getRootCertificate() throws CertificateEncodingException, KeyStoreException {
        return intermediateCA.getRootCertificate();
    }

    @GET
    @Path("intermediateca.crl")
    @Produces("application/pkix-crl")
    public String getCrl() throws GeneralSecurityException, IOException, OperatorCreationException {
        return intermediateCA.getCRL();
    }
}
