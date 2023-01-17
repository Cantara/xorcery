package com.exoreaction.xorcery.service.resources.api;

import com.exoreaction.xorcery.jsonapi.server.resources.JsonApiResource;
import com.exoreaction.xorcery.service.jetty.server.security.jwt.JwtUserPrincipal;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("api/principal")
public class UserPrincipalResource
        extends JsonApiResource {

    @GET
    public String get() {
        JwtUserPrincipal jwtUserPrincipal = (JwtUserPrincipal) getSecurityContext().getUserPrincipal();
        String tenant = jwtUserPrincipal.getClaims().get("tenant", String.class);
        return jwtUserPrincipal.getClaims().toString();
    }
}
