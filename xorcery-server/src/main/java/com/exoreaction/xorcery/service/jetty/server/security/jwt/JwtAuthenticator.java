package com.exoreaction.xorcery.service.jetty.server.security.jwt;

import com.exoreaction.xorcery.configuration.model.Configuration;
import io.jsonwebtoken.*;
import jakarta.inject.Inject;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.HttpHeaders;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.security.*;
import org.eclipse.jetty.server.Authentication;
import org.eclipse.jetty.server.UserIdentity;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

import javax.security.auth.Subject;

@Service(name = "server.security.jwt")
@ContractsProvided(Authenticator.class)
public class JwtAuthenticator
        implements Authenticator {

    private final Logger logger = LogManager.getLogger(JwtAuthenticator.class);

    private Configuration configuration;
    private SigningKeyResolver signingKeyResolver;
    private JwtParser jwtParser;

    @Inject
    public JwtAuthenticator(Configuration configuration, SigningKeyResolver signingKeyResolver) {

        this.configuration = configuration;
        this.signingKeyResolver = signingKeyResolver;
    }

    @Override
    public void setConfiguration(AuthConfiguration authConfiguration) {

        jwtParser = Jwts.parserBuilder()
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
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring("Bearer ".length());
        }
        return null;
    }
}
