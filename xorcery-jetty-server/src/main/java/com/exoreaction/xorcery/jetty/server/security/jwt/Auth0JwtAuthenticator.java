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
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.secrets.Secrets;
import jakarta.inject.Inject;
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
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

@Service(name = "jetty.server.security.jwt")
@ContractsProvided(Authenticator.class)
public class Auth0JwtAuthenticator
        implements Authenticator {

    private final Logger logger = LogManager.getLogger(Auth0JwtAuthenticator.class);

    private final JWT jwtParser;
    private final Map<String, Map<String, JWTVerifier>> verifiers = new HashMap<>();

    @Inject
    public Auth0JwtAuthenticator(Configuration configuration, LoginService loginService, Secrets secrets) throws NoSuchAlgorithmException {
        jwtParser = new JWT();
        JwtConfiguration jwtConfiguration = new JwtConfiguration(configuration.getConfiguration("jetty.server.security.jwt"));

        KeyFactory rsaKeyFactory = KeyFactory.getInstance("RSA");
        KeyFactory ecKeyFactory = KeyFactory.getInstance("EC");

        jwtConfiguration.getIssuers().forEach((issuer, config) ->
        {
            try {
                Map<String, JWTVerifier> issuerVerifiers = new HashMap<>();
                String defaultKid = "default";
                int defaultKidNr = 2;
                for (IssuerConfiguration.JwtKey key : config.getKeys()) {
                    byte[] publicKeyBase64 = secrets.getSecretBytes(key.getKey());
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
                            .acceptExpiresAt(1000L*3600L*800L)
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
    public void setConfiguration(AuthConfiguration authConfiguration) {
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
                DecodedJWT decodedJwt = jwtParser.decodeJwt(jwt);

                Map<String, JWTVerifier> issuerVerifiers = this.verifiers.getOrDefault(Optional.ofNullable(decodedJwt.getIssuer()).orElse("default"), Collections.emptyMap());

                JWTVerifier verifier = null;
                if (decodedJwt.getKeyId() != null) {
                    verifier = issuerVerifiers.get(decodedJwt.getKeyId());
                }
                if (verifier != null) {
                    // We found the correct key
                    try {
                        verifier.verify(decodedJwt);
                    } catch (JWTVerificationException e) {
                        logger.warn("Could not authenticate JWT token", e);
                        return Authentication.UNAUTHENTICATED;
                    }
                } else {
                    // Try all of them
                    JWTVerificationException error = new JWTVerificationException("Could not find JWT verifier for token:" + decodedJwt.getToken());
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
                        logger.warn("Could not find JWT verifier for token:" + decodedJwt.getToken());
                        return Authentication.UNAUTHENTICATED;
                    }
                }

                JwtCredential claimsCredential = new JwtCredential(decodedJwt);
                UserPrincipal userPrincipal = new JwtUserPrincipal(decodedJwt.getSubject(), claimsCredential);
                Subject subject = new Subject();
                userPrincipal.configureSubject(subject);
                subject.setReadOnly();

                List<String> roles = (List<String>)decodedJwt.getClaim("claims").asMap().get("roles");
                if (roles == null)
                    roles = Collections.emptyList();
                UserIdentity userIdentity = new DefaultUserIdentity(subject, userPrincipal, roles.toArray(new String[0]));
                authentication = new UserAuthentication(getAuthMethod(), userIdentity);

            } catch (Exception e) {
                logger.warn("Could not authenticate JWT token", e);
                return Authentication.UNAUTHENTICATED;
            }
        }

        return Objects.requireNonNullElse(authentication, Authentication.UNAUTHENTICATED);
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
