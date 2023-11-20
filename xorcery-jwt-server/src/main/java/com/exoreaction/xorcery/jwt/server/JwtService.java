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
package com.exoreaction.xorcery.jwt.server;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.configuration.InstanceConfiguration;
import com.exoreaction.xorcery.jwt.server.JwtServerConfiguration.JwtKey;
import com.exoreaction.xorcery.jwt.server.spi.ClaimsProvider;
import com.exoreaction.xorcery.secrets.Secrets;
import com.exoreaction.xorcery.server.api.ServiceResourceObject;
import com.exoreaction.xorcery.server.api.ServiceResourceObjects;
import com.exoreaction.xorcery.util.UUIDs;
import jakarta.inject.Inject;
import org.apache.logging.log4j.Logger;
import org.glassfish.hk2.api.IterableProvider;
import org.jvnet.hk2.annotations.Service;

import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service(name = "jwt.server")
public class JwtService {

    private final Logger logger;
    private final IterableProvider<ClaimsProvider> claimsProviders;

    private final String keyId;
    private final Algorithm algorithm;
    private final JwtServerConfiguration jwtServerConfiguration;
    private final Duration tokenDuration;

    @Inject
    public JwtService(Configuration configuration,
                      ServiceResourceObjects serviceResourceObjects,
                      Secrets secrets,
                      Logger logger,
                      IterableProvider<ClaimsProvider> claimsProviders) throws NoSuchAlgorithmException, InvalidKeySpecException {

        this.jwtServerConfiguration = new JwtServerConfiguration(configuration.getConfiguration("jwt.server"));

        List<JwtKey> keys = jwtServerConfiguration.getKeys();
        this.logger = logger;
        this.claimsProviders = claimsProviders;

        this.tokenDuration = jwtServerConfiguration.getTokenDuration();

        if (keys.isEmpty())
            throw new IllegalArgumentException("No signing keys configured");

        JwtKey firstKey = keys.get(0);
        byte[] keyBytes = Base64.getDecoder().decode(secrets.getSecretBytes(firstKey.getPrivateKey()));

        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance(firstKey.getAlg().startsWith("RSA") ? "RSA" : "EC");

        keyId = firstKey.getKeyId();

        if (firstKey.getAlg().startsWith("RSA")) {
            RSAPrivateKey privateKey = (RSAPrivateKey) kf.generatePrivate(keySpec);
            algorithm = switch (firstKey.getAlg()) {
                case "RSA256" -> Algorithm.RSA256(privateKey);
                case "RSA384" -> Algorithm.RSA384(privateKey);
                case "RSA512" -> Algorithm.RSA512(privateKey);
                default -> Algorithm.RSA256(privateKey);
            };
        } else if (firstKey.getAlg().startsWith("ES")) {
            ECPrivateKey privateKey = (ECPrivateKey) kf.generatePrivate(keySpec);
            algorithm = switch (firstKey.getAlg()) {
                case "ES256" -> Algorithm.ECDSA256(privateKey);
                case "ES384" -> Algorithm.ECDSA384(privateKey);
                case "ES512" -> Algorithm.ECDSA512(privateKey);
                default -> Algorithm.ECDSA256(privateKey);
            };
        } else {
            throw new IllegalArgumentException("Unknown algorithm:" + firstKey.getAlg());
        }

        serviceResourceObjects.add(new ServiceResourceObject.Builder(new InstanceConfiguration(configuration.getConfiguration("instance")), "login")
                .with(b ->
                {
                    b.api("login", "api/login");
                })
                .build());
    }

    public String createJwt(String userName)
            throws IOException {
        Instant now = Instant.now();
        Date expiresAt = Date.from(now.plus(tokenDuration));

        // Get all the claims for this user
        Map<String, Object> claims = new HashMap<>();
        for (ClaimsProvider claimsProvider : claimsProviders) {
            Map<String, ?> providerUserClaims = claimsProvider.getClaims(userName);
            claims.putAll(providerUserClaims);
        }

        String sub = (String)claims.remove("sub"); // Check if claims contains sub
        if (sub == null)
            sub = userName; // Otherwise use username

        String token = JWT.create()
                .withPayload(claims)
                .withIssuer(jwtServerConfiguration.getTokenIssuer())
                .withIssuedAt(now)
                .withExpiresAt(expiresAt)
                .withSubject(sub)
                .withKeyId(keyId)
                .withJWTId(UUIDs.newId())
                .sign(algorithm);

        logger.debug("Created JWT token for {}:{}", userName, token);
        return token;
    }

    public JwtServerConfiguration getJwtServerConfiguration() {
        return jwtServerConfiguration;
    }
}
