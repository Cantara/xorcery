package com.exoreaction.xorcery.service.jetty.server.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import org.eclipse.jetty.util.security.Credential;

public class ClaimsCredential
    extends Credential
{
    private Claims claims;

    public ClaimsCredential(Claims claims) {
        this.claims = claims;
    }

    @Override
    public boolean check(Object credentials) {
        return true;
    }

    public Claims getClaims() {
        return claims;
    }

    @Override
    public String toString() {
        return Jwts.builder().setClaims(claims).compact();
    }
}
