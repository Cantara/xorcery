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
package com.exoreaction.xorcery.jetty.server.security.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.exoreaction.xorcery.secrets.Secrets;
import jakarta.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.http.HttpCookie;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.security.*;
import org.eclipse.jetty.security.authentication.LoginAuthenticator;
import org.eclipse.jetty.security.authentication.SessionAuthentication;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Session;
import org.eclipse.jetty.util.Callback;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

import javax.security.auth.Subject;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;

import static org.eclipse.jetty.server.Request.getCookies;

@Service(name = "jetty.server.security.jwt")
@ContractsProvided(Authenticator.class)
public class Auth0JwtAuthenticator
        extends LoginAuthenticator {

    public static final String JWT_AUTH = "JWT";

    private final Logger logger = LogManager.getLogger(Auth0JwtAuthenticator.class);

    private final JWT jwtParser;
    private final Map<String, Map<String, JWTVerifier>> verifiers = new HashMap<>();
    private final JwtConfiguration jwtConfiguration;

    private String loginPath;
    private String loginPage;
    private String errorPage;
    private String errorPath;

    @Inject
    public Auth0JwtAuthenticator(com.exoreaction.xorcery.configuration.Configuration configuration, Secrets secrets) throws NoSuchAlgorithmException {
        jwtParser = new JWT();
        jwtConfiguration = new JwtConfiguration(configuration.getConfiguration("jetty.server.security.jwt"));

        jwtConfiguration.getLoginPage().ifPresent(this::setLoginPage);
        jwtConfiguration.getErrorPage().ifPresent(this::setErrorPage);

        KeyFactory rsaKeyFactory = KeyFactory.getInstance("RSA");
        KeyFactory ecKeyFactory = KeyFactory.getInstance("EC");

        jwtConfiguration.getIssuers().forEach((issuer, config) ->
        {
            try {
                Map<String, JWTVerifier> issuerVerifiers = new HashMap<>();
                String defaultKid = "default";
                int defaultKidNr = 2;
                for (IssuerConfiguration.JwtKey key : config.getKeys()) {
                    byte[] publicKeyBase64 = secrets.getSecretBytes(key.getPublicKey());
                    byte[] keyBytes = Base64.getDecoder().decode(publicKeyBase64);
                    X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);

                    Algorithm algorithm = null;
                    if (key.getAlg().startsWith("RS")) {
                        RSAPublicKey publicKey = (RSAPublicKey) rsaKeyFactory.generatePublic(keySpec);
                        algorithm = switch (key.getAlg()) {
                            case "RS256" -> Algorithm.RSA256(publicKey);
                            case "RS384" -> Algorithm.RSA384(publicKey);
                            case "RS512" -> Algorithm.RSA512(publicKey);
                            default -> Algorithm.RSA256(publicKey);
                        };
                    } else if (key.getAlg().startsWith("ES")) {
                        ECPublicKey publicKey = (ECPublicKey) ecKeyFactory.generatePublic(keySpec);
                        algorithm = switch (key.getAlg()) {
                            case "ES256" -> Algorithm.ECDSA256(publicKey);
                            case "ES384" -> Algorithm.ECDSA384(publicKey);
                            case "ES512" -> Algorithm.ECDSA512(publicKey);
                            default -> Algorithm.ECDSA256(publicKey);
                        };
                    } else {
                        throw new IllegalArgumentException("Unknown algorithm:" + key.getAlg());
                    }

                    // TODO Allow config to specify required claims
                    JWTVerifier verifier = JWT.require(algorithm)
                            .acceptExpiresAt(1000L * 3600L * 800L)
                            .build();
                    String keyId = key.getKid().orElse(null);
                    if (keyId == null) {
                        keyId = defaultKid;
                        defaultKid = "default" + defaultKidNr;
                        defaultKidNr++;
                    }
                    issuerVerifiers.put(keyId, verifier);
                }
                verifiers.put(issuer, issuerVerifiers);
            } catch (InvalidKeySpecException e) {
                throw new IllegalArgumentException("Could not initialize JWT algorithms", e);
            }
        });
    }

    @Override
    public void setConfiguration(Configuration authConfiguration) {
        super.setConfiguration(authConfiguration);
        // TODO Add config checks here? Do we need it considering the Xorcery config?

    }

    @Override
    public String getAuthenticationType() {
        return JWT_AUTH;
    }

    private void setLoginPage(String path) {
        if (!path.startsWith("/")) {
            logger.warn("form-login-page must start with /");
            path = "/" + path;
        }
        loginPage = path;
        loginPath = path;
        if (loginPath.indexOf('?') > 0)
            loginPath = loginPath.substring(0, loginPath.indexOf('?'));
    }

    private void setErrorPage(String path) {
        if (path == null || path.isBlank()) {
            errorPath = null;
            errorPage = null;
        } else {
            if (!path.startsWith("/")) {
                logger.warn("form-error-page must start with /");
                path = "/" + path;
            }
            errorPage = path;
            errorPath = path;

            if (errorPath.indexOf('?') > 0)
                errorPath = errorPath.substring(0, errorPath.indexOf('?'));
        }
    }

    @Override
    public UserIdentity login(String username, Object password, Request request, Response response) {
        UserIdentity user = super.login(username, password, request, response);
        if (user != null) {
            Session session = request.getSession(true);
            AuthenticationState cached = new SessionAuthentication(getAuthenticationType(), user, password);
            session.setAttribute(SessionAuthentication.AUTHENTICATED_ATTRIBUTE, cached);
        }
        return user;
    }

    @Override
    public void logout(Request request, Response response) {
        super.logout(request, response);
        Session session = request.getSession(false);
        if (session == null)
            return;

        // Clean up session
        session.removeAttribute(SessionAuthentication.AUTHENTICATED_ATTRIBUTE);

        // Remove JWT token cookie
        List<HttpCookie> cookies = getCookies(request);
        for (HttpCookie cookie : cookies) {
            if (cookie.getName().equals(jwtConfiguration.getTokenCookieName())) {
                HttpCookie tokenCookie = HttpCookie.build(cookie).expires(Instant.now()).build();
                HttpFields.Mutable trailers = HttpFields.build(HttpFields.from(new HttpField(HttpHeader.COOKIE, HttpCookie.toString(tokenCookie))));
                response.setTrailersSupplier(() -> trailers);
            }
        }
    }

    @Override
    public Request prepareRequest(Request request, AuthenticationState authenticationState) {
        // TODO Figure out what to do here
        return request;
    }

    @Override
    public Constraint.Authorization getConstraintAuthentication(String pathInContext, Constraint.Authorization existing, Function<Boolean, Session> getSession) {
        if (isLoginOrErrorPage(pathInContext))
            return Constraint.Authorization.ALLOWED;
        return existing;
    }

    @Override
    public AuthenticationState validateRequest(Request request, Response response, Callback callback) throws ServerAuthException {
        String jwt = getBearerToken(request);

        if (jwt == null)
            jwt = getCookieToken(request);

        if (jwt != null) {
            try {
                DecodedJWT decodedJwt = jwtParser.decodeJwt(jwt);

                // Check expiration date
                if (decodedJwt.getExpiresAtAsInstant().isBefore(Instant.now()))
                    throw new ServerAuthException("JWT token is expired");

                Map<String, JWTVerifier> issuerVerifiers = this.verifiers.getOrDefault(Optional.ofNullable(decodedJwt.getIssuer()).orElse("default"), Collections.emptyMap());

                JWTVerifier verifier = null;
                if (decodedJwt.getKeyId() != null) {
                    verifier = issuerVerifiers.get(decodedJwt.getKeyId());
                }

                // Verify JWT token
                if (verifier != null) {
                    // We found the correct key
                    try {
                        verifier.verify(decodedJwt);
                    } catch (JWTVerificationException e) {
                        logger.warn("Could not authenticate JWT token", e);
                        throw new ServerAuthException("Could not authenticate JWT token", e);
                    }
                } else {
                    // Try all of them
                    JWTVerificationException error = new JWTVerificationException(String.format("Could not find JWT verifier for token, kid:%s,issuer:%s, sub:%s", decodedJwt.getKeyId(), decodedJwt.getIssuer(), decodedJwt.getSubject()));
                    for (JWTVerifier jwtVerifier : issuerVerifiers.values()) {
                        try {
                            jwtVerifier.verify(decodedJwt);
                            error = null;
                            break;
                        } catch (JWTVerificationException e) {
                            error = e;
                        }
                    }

                    if (error != null) {
                        throw new ServerAuthException("Could not authenticate JWT token", error);
                    }
                }

                // Check if already logged in
                // Look for cached authentication
                Session session = request.getSession(false);
                AuthenticationState authenticationState = session == null ? null : (AuthenticationState) session.getAttribute(SessionAuthentication.AUTHENTICATED_ATTRIBUTE);
                if (logger.isDebugEnabled())
                    logger.debug("auth {}", authenticationState);

                // Has authentication been revoked?
                if (authenticationState instanceof AuthenticationState.Succeeded succeeded && _loginService != null && !_loginService.validate(succeeded.getUserIdentity())) {
                    if (logger.isDebugEnabled())
                        logger.debug("auth revoked {}", authenticationState);
                    session.removeAttribute(SessionAuthentication.AUTHENTICATED_ATTRIBUTE);
                    authenticationState = null;
                }

                // Found valid authentication, return it
                if (authenticationState != null)
                    return authenticationState;

                // Login user
                JwtCredential claimsCredential = new JwtCredential(decodedJwt);
                UserPrincipal userPrincipal = new JwtUserPrincipal(decodedJwt.getSubject(), claimsCredential);
                Subject subject = new Subject();
                userPrincipal.configureSubject(subject);
                subject.setReadOnly();

                List<String> roles = decodedJwt.getClaim("roles").asList(String.class);
                if (roles == null)
                    roles = Collections.emptyList();
                UserIdentity userIdentity = UserIdentity.from(subject, userPrincipal, roles.toArray(new String[0]));
                return new LoginAuthenticator.UserAuthenticationSucceeded(getAuthenticationType(), userIdentity);
            } catch (ServerAuthException e) {
                throw e;
            } catch (Exception e) {
                logger.warn("Could not authenticate JWT token", e);
                throw new ServerAuthException("Could not authenticate JWT token", e);
            }
        }

        return AuthenticationState.defer(this);
    }

    private String getBearerToken(Request request) {
        String authorizationHeader = request.getHeaders().get(HttpHeader.AUTHORIZATION.lowerCaseName());
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring("Bearer ".length());
        }
        return null;
    }

    private String getCookieToken(Request request) {
        List<HttpCookie> cookies = Request.getCookies(request);
        if (cookies == null)
            return null;
        for (HttpCookie cookie : cookies) {
            if (cookie.getName().equals(jwtConfiguration.getTokenCookieName()))
                return cookie.getValue();
        }
        return null;
    }

    private boolean isLoginOrErrorPage(String pathInContext) {
        return pathInContext != null && (pathInContext.equals(errorPath) || pathInContext.equals(loginPath));
    }
}
