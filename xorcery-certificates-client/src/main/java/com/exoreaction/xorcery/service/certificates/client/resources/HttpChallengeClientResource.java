package com.exoreaction.xorcery.service.certificates.client.resources;

import jakarta.inject.Inject;
import jakarta.ws.rs.Path;

@Path(".well-known/acme-challenge/{id}")
public class HttpChallengeClientResource {

    @Inject
    public HttpChallengeClientResource() {
    }
}
