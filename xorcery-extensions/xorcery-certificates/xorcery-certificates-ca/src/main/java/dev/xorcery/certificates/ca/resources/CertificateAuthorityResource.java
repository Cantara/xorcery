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
package dev.xorcery.certificates.ca.resources;

import dev.xorcery.certificates.ca.IntermediateCACertificatesProvider;
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
