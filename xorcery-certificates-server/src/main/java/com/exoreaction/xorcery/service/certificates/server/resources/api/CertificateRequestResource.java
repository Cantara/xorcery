package com.exoreaction.xorcery.service.certificates.server.resources.api;

import com.exoreaction.xorcery.jsonapi.model.Attributes;
import com.exoreaction.xorcery.jsonapi.model.ResourceObject;
import com.exoreaction.xorcery.jsonapi.server.resources.JsonApiResource;
import com.exoreaction.xorcery.service.certificates.server.IntermediateCA;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCSException;

import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Path("api/certificates/request")
@Singleton
public class CertificateRequestResource
        extends JsonApiResource {

    private static final Logger logger = LogManager.getLogger(CertificateRequestResource.class);

    private IntermediateCA intermediateCA;

    @Inject
    public CertificateRequestResource(IntermediateCA intermediateCA) {
        this.intermediateCA = intermediateCA;
    }

    @GET
    public ResourceObject get() {
        return new ResourceObject.Builder("certificaterequest")
                .attributes(new Attributes.Builder()
                        .attribute("csr", "")
                        .build()).build();
    }

    @POST
    public ResourceObject post(ResourceObject csr) throws CertificateException, IOException, ExecutionException, InterruptedException, KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException, OperatorCreationException, PKCSException {

        String csrPem = csr.getAttributes().getString("csr").orElseThrow(() -> new IllegalArgumentException("Missing CSR PEM attribute"));
        String certificatePem = intermediateCA.requestCertificate(csrPem, List.of("127.0.0.1", "192.168.1.107"));

        return new ResourceObject.Builder("certificate")
                .attributes(new Attributes.Builder()
                        .attribute("pem", certificatePem)
                        .build()).build();
    }
}
