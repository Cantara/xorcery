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
package dev.xorcery.test.jetty;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import dev.xorcery.configuration.Configuration;
import dev.xorcery.configuration.InstanceConfiguration;
import dev.xorcery.configuration.builder.ConfigurationBuilder;
import dev.xorcery.core.Xorcery;
import dev.xorcery.jsonapi.MediaTypes;
import dev.xorcery.net.Sockets;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.HttpHeaders;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.interfaces.ECKey;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

public class JwtAuthenticationTest {
    String conxfig = """
            $schema: META-INF/xorcery-override-schema.json
            
            jetty.server.enabled: true
            jetty.client.enabled: true
            jetty.server.security.enabled: true
            jetty.server.security.method: "jwt"
            jetty.server.security.jwt:
                enabled: true
                issuers:
                  authentication.xorcery.test:
                    keys:
                      - kid: "{{SYSTEM.kid}}"
                        alg: "ES256"
                        publicKey: "secret:{{SYSTEM.key}}"
            opentelemetry.enabled: true
            opentelemetry.instrumentations.jersey.enabled: true
            """;

    @Inject
    ClientBuilder clientBuilder;

    @Test
    public void testValidJWTAuthentication() throws Exception {
        KeyPairGenerator g = KeyPairGenerator.getInstance("EC");
        g.initialize(256);
        KeyPair keyPair = g.generateKeyPair();

        String publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        String keyId = UUID.randomUUID().toString();
        System.setProperty("kid", keyId);
        System.setProperty("key", publicKey);
        System.setProperty("port", Integer.toString(Sockets.nextFreePort()));
        Configuration serverConfiguration = new ConfigurationBuilder().addTestDefaults().addResource("JwtAuthenticationTest.yaml").build();
        System.out.println(serverConfiguration);
        try (Xorcery server = new Xorcery(serverConfiguration)) {

            server.getServiceLocator().inject(this);

            InstanceConfiguration cfg = new InstanceConfiguration(serverConfiguration.getConfiguration("instance"));
            String jwt = createJwt(keyId, keyPair.getPrivate());
            try (Client client = clientBuilder.build()) {
                String response = client.target(cfg.getAPI().resolve("subject")).request().accept(MediaTypes.APPLICATION_JSON_API).header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt).get().readEntity(String.class);

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
