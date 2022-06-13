package com.exoreaction.reactiveservices.service.handlebars.resources;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.*;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Path("api/handlebars/uritemplate")
public class UriTemplateResource {

    @GET
    public Response get(@Context UriInfo uriInfo) {
        MultivaluedMap<String, String> queryParameters = uriInfo.getQueryParameters();
        Map<String, Object> cleanedParameters = new HashMap<>();
        for (String name : queryParameters.keySet()) {
            cleanedParameters.put(name, Optional.ofNullable(queryParameters.getFirst(name)).orElse(""));
        }
        URI redirectUri = UriBuilder.fromUri(queryParameters.getFirst("uritemplate"))
                .resolveTemplates(cleanedParameters).build();
        return Response.temporaryRedirect(redirectUri).build();
    }
}
