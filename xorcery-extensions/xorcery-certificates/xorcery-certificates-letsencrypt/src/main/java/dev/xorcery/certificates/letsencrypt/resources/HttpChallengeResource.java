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
package dev.xorcery.certificates.letsencrypt.resources;

import dev.xorcery.certificates.letsencrypt.LetsEncryptCertificatesManagementProvider;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

@Path(".well-known/acme-challenge/{id}")
public class HttpChallengeResource {

    private final LetsEncryptCertificatesManagementProvider letsEncryptCertificatesManagementProvider;

    @Inject
    public HttpChallengeResource(LetsEncryptCertificatesManagementProvider letsEncryptCertificatesManagementProvider) {
        this.letsEncryptCertificatesManagementProvider = letsEncryptCertificatesManagementProvider;
    }

    @GET
    public Response get(@PathParam("id") String id) {
        return Response.ok().entity(letsEncryptCertificatesManagementProvider.getHttpAuthorization(id)).build();
    }
}
