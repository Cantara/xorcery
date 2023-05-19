/*
 * Copyright © 2022 eXOReaction AS (rickard@exoreaction.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.exoreaction.xorcery.service.certificates.server.resources;

import com.exoreaction.xorcery.jsonapi.model.Attributes;
import com.exoreaction.xorcery.jsonapi.model.ResourceObject;
import com.exoreaction.xorcery.jsonapi.server.resources.JsonApiResource;
import com.exoreaction.xorcery.service.certificates.server.CertificatesServer;
import com.exoreaction.xorcery.service.certificates.server.ServerCertificatesProvider;
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
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

@Path("api/certificates/request")
@Singleton
public class CertificateRequestResource
        extends JsonApiResource {

    private ServerCertificatesProvider certificatesServer;

    @Inject
    public CertificateRequestResource(ServerCertificatesProvider certificatesServer) {
        this.certificatesServer = certificatesServer;
    }

    @GET
    public ResourceObject get() {
        return new ResourceObject.Builder("certificaterequest")
                .attributes(new Attributes.Builder()
                        .attribute("csr", "")
                        .build()).build();
    }

    @POST
    public CompletionStage<ResourceObject> post(ResourceObject csr) {
        return csr.getAttributes().getString("csr")
                .map(csrPem -> certificatesServer.requestCertificate(csrPem)
                        .thenApply(certificatesPem -> new ResourceObject.Builder("certificate")
                                .attributes(new Attributes.Builder()
                                        .attribute("pem", certificatesPem)
                                        .build()).build()))
                .orElseGet(() -> CompletableFuture.failedStage(new IllegalArgumentException("Missing CSR PEM attribute")));
    }
}
