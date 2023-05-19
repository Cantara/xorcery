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
package com.exoreaction.xorcery.service.jetty.server.security.jwt;

import io.jsonwebtoken.*;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.security.*;
import org.eclipse.jetty.server.Authentication;
import org.eclipse.jetty.server.UserIdentity;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

import javax.security.auth.Subject;
import java.time.Clock;
import java.util.Date;
import java.util.Optional;

@Service(name = "jetty.server.security.jwt")
@ContractsProvided(Authenticator.class)
public class JwtAuthenticator
        implements Authenticator {

    private final Logger logger = LogManager.getLogger(JwtAuthenticator.class);

    private SigningKeyResolver signingKeyResolver;
    private java.time.Clock clock;
    private JwtParser jwtParser;

    @Inject
    public JwtAuthenticator(SigningKeyResolver signingKeyResolver, Provider<Clock> clock) {
        this.signingKeyResolver = signingKeyResolver;
        this.clock = Optional.ofNullable(clock.get()).orElse(Clock.systemUTC());
    }

    @Override
    public void setConfiguration(AuthConfiguration authConfiguration) {

        jwtParser = Jwts.parserBuilder()
                .setClock(() -> new Date(clock.millis()))
                .setSigningKeyResolver(signingKeyResolver)
                .build();
    }

    @Override
    public String getAuthMethod() {
        return "jwt";
    }

    @Override
    public void prepareRequest(ServletRequest request) {
    }

    @Override
    public Authentication validateRequest(ServletRequest servletRequest, ServletResponse response, boolean mandatory) throws ServerAuthException {
        final HttpServletRequest request = (HttpServletRequest) servletRequest;

        UserAuthentication authentication = null;

        String jwt = getBearerToken(request);

        if (jwt == null)
            jwt = getCookieToken(request);

        if (jwt != null) {
            try {
                final Jws<Claims> jws = jwtParser.parseClaimsJws(jwt);

                ClaimsCredential claimsCredential = new ClaimsCredential(jws.getBody());
                UserPrincipal userPrincipal = new JwtUserPrincipal(jws.getBody().getSubject(), claimsCredential);
                Subject subject = new Subject();
                userPrincipal.configureSubject(subject);
                subject.setReadOnly();

                UserIdentity userIdentity = new DefaultUserIdentity(subject, userPrincipal, new String[0]);
                authentication = new UserAuthentication(getAuthMethod(), userIdentity);

            } catch (Exception e) {
                logger.error("Could not authenticate JWT token", e);
                return Authentication.UNAUTHENTICATED;
            }
        }

        if (authentication != null) {
            return authentication;
        } else {
            return Authentication.UNAUTHENTICATED;
        }
    }

    @Override
    public boolean secureResponse(ServletRequest request, ServletResponse response, boolean mandatory, Authentication.User validatedUser) throws ServerAuthException {
        return true;
    }

    private String getBearerToken(HttpServletRequest request) {
        String authorizationHeader = request.getHeader(HttpHeader.AUTHORIZATION.lowerCaseName());
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring("Bearer ".length());
        }
        return null;
    }

    private String getCookieToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null)
            return null;
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals("token"))
                return cookie.getValue();
        }
        return null;
    }
}
