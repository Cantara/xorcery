package com.exoreaction.xorcery.service.certificates.server.resources.api;

import com.exoreaction.xorcery.service.certificates.server.IntermediateCA;
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

@Path("api/certificates")
@Singleton
public class CertificatesResource {

    private IntermediateCA intermediateCA;

    @Inject
    public CertificatesResource(IntermediateCA intermediateCA) {
        this.intermediateCA = intermediateCA;
    }

    @GET
    @Path("rootca.cer")
    @Produces("application/x-x509-ca-cert")
    public byte[] getRootCa() throws CertificateEncodingException, KeyStoreException {
        return intermediateCA.getCertificate();
    }

    @GET
    @Path("crl")
    @Produces("application/x-x509-ca-cert")
    public String getCrl() throws GeneralSecurityException, IOException, OperatorCreationException {
        return intermediateCA.getCRL();
    }
}
