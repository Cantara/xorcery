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
package com.exoreaction.xorcery.jetty.server.security.jwt.test;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.configuration.InstanceConfiguration;
import com.exoreaction.xorcery.configuration.builder.ConfigurationBuilder;
import com.exoreaction.xorcery.core.Xorcery;
import com.exoreaction.xorcery.jsonapi.MediaTypes;
import com.exoreaction.xorcery.net.Sockets;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.HttpHeaders;
import org.apache.shiro.codec.Base64;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.interfaces.ECKey;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

public class JwtAuthenticationTest {
    String config = """
            dns.client.enabled: true
            dns.client.discovery.enabled: false
            keystores.enabled: true
            jetty.server.http.enabled: true
            jetty.server.http.port: "{{ SYSTEM.port }}"
            jetty.server.ssl.enabled: false
            jetty.server.security.enabled: true
            jetty.server.security.method: "jwt"
            jetty.server.security.jwt:
                enabled: true
                issuers:
                  authentication.xorcery.test:
                    keys:
                      - alg: "ES256"
                        kid: "{{SYSTEM.kid}}"
                        key: "{{SYSTEM.key}}"                        
            """;

    @Inject
    ClientBuilder clientBuilder;

    @Test
    public void testValidJWTAuthentication() throws Exception {
        KeyPairGenerator g = KeyPairGenerator.getInstance("EC");
        g.initialize(256);
        KeyPair keyPair = g.generateKeyPair();

        String publicKey = Base64.encodeToString(keyPair.getPublic().getEncoded());
        String keyId = UUID.randomUUID().toString();
        System.setProperty("kid", keyId);
        System.setProperty("key", publicKey);
        System.setProperty("port", Integer.toString(Sockets.nextFreePort()));
        Configuration serverConfiguration = new ConfigurationBuilder().addTestDefaults().addYaml(config).build();
        System.out.println(serverConfiguration);
        try (Xorcery server = new Xorcery(serverConfiguration)) {

            server.getServiceLocator().inject(this);

            InstanceConfiguration cfg = new InstanceConfiguration(serverConfiguration.getConfiguration("instance"));
            String jwt = createJwt(keyId, keyPair.getPrivate());
            try (Client client = clientBuilder.build()) {
                String response = client.target(cfg.getURI().resolve("api/subject")).request().accept(MediaTypes.APPLICATION_JSON_API).header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt).get().readEntity(String.class);

                System.out.println(response);
            }
        }
    }

    private String createJwt(String keyId, PrivateKey key) {
        Date now = new Date();
        Date tomorrow = Date.from(now.toInstant().plus(1, ChronoUnit.DAYS));

        Algorithm algorithm = Algorithm.ECDSA256((ECKey) key);
        String token = JWT.create()
                .withIssuer("authentication.xorcery.test")
                .withKeyId(keyId)
                .withSubject("gandalf")
                .withClaim("name", "Gandalf")
                .withClaim("scope", "users")
                .withClaim("tenant", "SomeTenant")
                .withIssuedAt(now)
                .withExpiresAt(tomorrow)
                .sign(algorithm);
        return token;
    }

}
