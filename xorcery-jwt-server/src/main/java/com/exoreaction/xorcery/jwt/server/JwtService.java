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
import com.auth0.jwt.interfaces.DecodedJWT;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.configuration.InstanceConfiguration;
import com.exoreaction.xorcery.json.JsonElement;
import com.exoreaction.xorcery.secrets.Secrets;
import com.exoreaction.xorcery.server.api.ServiceResourceObjects;
import com.exoreaction.xorcery.server.api.ServiceResourceObject;
import com.exoreaction.xorcery.util.UUIDs;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.http.HttpHeader;
import org.jvnet.hk2.annotations.Service;

import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static com.exoreaction.xorcery.configuration.Configuration.missing;

@Service(name = "jwt.server")
public class JwtService {

    private final List<JwtKey> keys;
    private final Logger logger;

    private final String keyId;
    private final Algorithm algorithm;
    private final Configuration users;
    private final JwtServerConfiguration jwtServerConfiguration;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // TODO: Change this to create signed JWT given username, change login service to ONLY authenticate username+password, change resource to only call createJwt(username)

    @Inject
    public JwtService(Configuration configuration,
                      ServiceResourceObjects serviceResourceObjects,
                      Secrets secrets,
                      Logger logger) throws NoSuchAlgorithmException, InvalidKeySpecException {

        this.users = configuration.getConfiguration("jwt.users");
        this.jwtServerConfiguration = new JwtServerConfiguration(configuration.getConfiguration("jwt.server"));

        keys = configuration.getObjectListAs("jwt.server.keys", JwtKey::new).orElse(Collections.emptyList());
        this.logger = logger;

        if (keys.isEmpty())
            throw new IllegalArgumentException("No signing keys configured");

        JwtKey firstKey = keys.get(0);
        byte[] keyBytes = Base64.getDecoder().decode(secrets.getByteSecret(firstKey.getPrivateKey()));

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
        Date now = new Date();
        Date tomorrow = Date.from(now.toInstant().plus(1, ChronoUnit.DAYS));
        if (!users.json().has(userName))
            throw new NotFoundException(userName);
        Configuration user = users.getConfiguration(userName);

        Map<String, ?> claims = objectMapper.treeToValue(user.getConfiguration("claims").json(), Map.class);

        String token = JWT.create()
                .withPayload(claims)
                .withIssuer(jwtServerConfiguration.getIssuer())
                .withIssuedAt(now)
                .withExpiresAt(tomorrow)
                .withSubject(userName)
                .withKeyId(keyId)
                .withJWTId(UUIDs.newId())
                .sign(algorithm);

        logger.debug("Created JWT token for {}:{}", userName, token);
        return token;
    }

    public record JwtKey(Configuration configuration) {
        public JwtKey(ObjectNode on) {
            this(new Configuration(on));
        }

        public String getAlg() {
            return configuration.getString("alg").orElse("ES256");
        }

        public String getKeyId() {
            return configuration.getString("keyId").orElseThrow(missing("keyId"));
        }

        public String getPublicKey() {
            return configuration.getString("publicKey").orElseThrow(missing("publicKey"));
        }

        public String getPrivateKey() {
            return configuration.getString("privateKey").orElseThrow(missing("privateKey"));
        }
    }
}
