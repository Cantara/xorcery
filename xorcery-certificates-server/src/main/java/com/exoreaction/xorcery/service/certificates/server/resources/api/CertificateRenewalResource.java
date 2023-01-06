package com.exoreaction.xorcery.service.certificates.server.resources.api;

import com.exoreaction.xorcery.jsonapi.model.Attributes;
import com.exoreaction.xorcery.jsonapi.model.ResourceDocument;
import com.exoreaction.xorcery.jsonapi.model.ResourceObject;
import com.exoreaction.xorcery.jsonapi.model.ResourceObjects;
import com.exoreaction.xorcery.jsonapi.server.resources.JsonApiResource;
import com.exoreaction.xorcery.service.certificates.server.IntermediateCA;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.operator.OperatorCreationException;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Path("api/certificates/renewal")
@Singleton
public class CertificateRenewalResource
        extends JsonApiResource {

    private static final Logger logger = LogManager.getLogger(CertificateRenewalResource.class);

    private IntermediateCA intermediateCA;

    @Inject
    public CertificateRenewalResource(IntermediateCA intermediateCA) {
        this.intermediateCA = intermediateCA;
    }

    @GET
    public ResourceDocument get() throws CertificateException, IOException, ExecutionException, InterruptedException, KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException, OperatorCreationException {

        ResourceObjects.Builder builder = new ResourceObjects.Builder();

        X509Certificate[] property = (X509Certificate[]) getContainerRequestContext().getProperty("jakarta.servlet.request.X509Certificate");
        if (property != null) {
            List<X509Certificate> certs = List.of(property);

            for (X509Certificate cert : certs) {

                logger.info("Current expiry date:" + cert.getNotAfter());

                Date tomorrow = Date.from((Instant) Duration.ofDays(1).addTo(Instant.now()));
                if (tomorrow.after(cert.getNotAfter()) || Optional.ofNullable(getFirstQueryParameter("force")).map(Boolean::parseBoolean).orElse(false)) {
                    String certificatePem = intermediateCA.renewCertificate(cert);
                    builder.resource(new ResourceObject.Builder("certificate")
                            .attributes(new Attributes.Builder()
                                    .attribute("pem", certificatePem)
                                    .build()).build());
                }
            }
        }

        return new ResourceDocument.Builder()
                .data(builder)
                .build();
    }
}
