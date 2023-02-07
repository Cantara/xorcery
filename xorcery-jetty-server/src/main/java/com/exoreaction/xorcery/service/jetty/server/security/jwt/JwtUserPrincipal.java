package com.exoreaction.xorcery.service.jetty.server.security.jwt;

import io.jsonwebtoken.Claims;
import org.eclipse.jetty.security.UserPrincipal;

public class JwtUserPrincipal
    extends UserPrincipal
{
    public JwtUserPrincipal(String name, ClaimsCredential credential) {
        super(name, credential);
    }

    public Claims getClaims()
    {
        return ((ClaimsCredential)_credential).getClaims();
    }
}
