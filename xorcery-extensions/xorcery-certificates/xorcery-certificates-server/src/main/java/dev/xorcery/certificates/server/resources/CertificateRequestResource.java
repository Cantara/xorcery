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
package dev.xorcery.certificates.server.resources;

import dev.xorcery.certificates.server.ServerCertificatesProvider;
import dev.xorcery.jaxrs.server.resources.BaseResource;
import dev.xorcery.jsonapi.Attributes;
import dev.xorcery.jsonapi.ResourceObject;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Path("api/certificates/request")
@Singleton
public class CertificateRequestResource
        extends BaseResource {

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
