package com.exoreaction.xorcery.service.certificates.letsencrypt.resources;

import com.exoreaction.xorcery.service.certificates.letsencrypt.LetsEncryptCertificatesManagementProvider;
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
