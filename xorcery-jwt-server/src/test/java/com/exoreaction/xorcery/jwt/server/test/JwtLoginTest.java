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
package com.exoreaction.xorcery.jwt.server.test;

import com.exoreaction.xorcery.configuration.builder.StandardConfigurationBuilder;
import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.configuration.model.InstanceConfiguration;
import com.exoreaction.xorcery.core.Xorcery;
import com.exoreaction.xorcery.jsonapi.client.JsonApiClient;
import com.exoreaction.xorcery.jsonapi.jaxrs.providers.JsonElementMessageBodyReader;
import com.exoreaction.xorcery.jsonapi.jaxrs.providers.JsonElementMessageBodyWriter;
import com.exoreaction.xorcery.jsonapi.model.Attributes;
import com.exoreaction.xorcery.jsonapi.model.Link;
import com.exoreaction.xorcery.jsonapi.model.ResourceObject;
import com.exoreaction.xorcery.util.Sockets;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.HttpHeaders;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;

public class JwtLoginTest {

    String config = """
            dns.client.enabled: true
            dns.client.discovery.enabled: false
            keystores.enabled: true
            jetty.server.ssl.enabled: false
            jetty.server.security.enabled: true
            jetty.server.security.method: "jwt"
            jetty.server.security.jwt.enabled: true                        
            """;

    @Inject
    ClientBuilder clientBuilder;

    @Test
    public void testJwtLogin() throws Exception {
        Logger logger = LogManager.getLogger(getClass());

        KeyPair keyPair = Keys.keyPairFor(SignatureAlgorithm.ES256);

        String publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        String privateKey = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());

        int managerPort = Sockets.nextFreePort();
        managerPort = 8443;
        Configuration serverConfiguration = new Configuration.Builder()
                .with(new StandardConfigurationBuilder().addTestDefaultsWithYaml(config))
                .add("jetty.server.http.port", Sockets.nextFreePort())
                .add("jetty.server.security.jwt.issuers", JsonNodeFactory.instance.objectNode().set("server.xorcery.test", JsonNodeFactory.instance.textNode(publicKey)))
                .add("jwt.server.key", privateKey)
                .build();
        System.out.println(StandardConfigurationBuilder.toYaml(serverConfiguration));
        try (Xorcery server = new Xorcery(serverConfiguration)) {

            server.getServiceLocator().inject(this);

            InstanceConfiguration cfg = new InstanceConfiguration(serverConfiguration.getConfiguration("instance"));
            String jwt = createJwt(keyPair.getPrivate());
            try (Client client = clientBuilder
                    .register(JsonElementMessageBodyReader.class)
                    .register(JsonElementMessageBodyWriter.class)
                    .build()) {
                JsonApiClient jsonApiClient = new JsonApiClient(client);
                jsonApiClient.get(new Link("login", cfg.getURI().resolve("api/login").toASCIIString()))
                        .thenCompose(rd -> rd.getResource().map(ro ->
                        {
                            return jsonApiClient.submit(rd.getLinks().getSelf().orElseThrow(), new ResourceObject.Builder(ro)
                                    .attributes(new Attributes.Builder(ro.getAttributes().json())
                                            .attribute("username", "foobar")
                                            .attribute("password", "secret")
                                            .build()).build());
                        }).orElseThrow(IllegalStateException::new))
                        .whenComplete((result, t) -> System.out.println(result)).toCompletableFuture().join();
            }
        }
    }

    private String createJwt(PrivateKey key) {
        Date now = new Date();
        Date tomorrow = Date.from(now.toInstant().plus(1, ChronoUnit.DAYS));

        String jws = Jwts.builder()
                .setIssuer("server.xorcery.test")
                .setSubject("gandalf")
                .claim("name", "Gandalf")
                .claim("scope", "users")
                .claim("tenant", "SomeTenant")
                // Fri Jun 24 2016 15:33:42 GMT-0400 (EDT)
                .setIssuedAt(now)
                // Sat Jun 24 2116 15:33:42 GMT-0400 (EDT)
                .setExpiration(tomorrow)
                .signWith(key, SignatureAlgorithm.ES256)
                .compact();
        return jws;
    }
}
