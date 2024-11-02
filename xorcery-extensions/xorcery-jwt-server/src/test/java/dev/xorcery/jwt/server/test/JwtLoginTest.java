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
package dev.xorcery.jwt.server.test;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import dev.xorcery.configuration.Configuration;
import dev.xorcery.configuration.InstanceConfiguration;
import dev.xorcery.configuration.builder.ConfigurationBuilder;
import dev.xorcery.core.Xorcery;
import dev.xorcery.jsonapi.Attributes;
import dev.xorcery.jsonapi.Link;
import dev.xorcery.jsonapi.ResourceObject;
import dev.xorcery.jsonapi.client.JsonApiClient;
import dev.xorcery.jsonapi.providers.JsonElementMessageBodyReader;
import dev.xorcery.jsonapi.providers.JsonElementMessageBodyWriter;
import dev.xorcery.net.Sockets;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled
public class JwtLoginTest {

    String config = """
            dns.client.enabled: true
            dns.client.discovery.enabled: false
            keystores.enabled: true
            jetty.server.ssl.enabled: false
            jetty.server.security.enabled: true
            jetty.server.security.type: "jwt"
            jetty.server.security.jwt.enabled: true                        
            """;

    String serverConfig = """
            dns.client.enabled: true
            dns.client.discovery.enabled: false
            keystores.enabled: true
            jetty.server.http.port: "{{SYSTEM.port}}"
            jetty.server.ssl.enabled: false
            jetty.server.security.enabled: true
            jetty.server.security.type: "jwt"
            jetty.server.security.jwt.enabled: true                        
            """;
    String clientConfig = """
            dns.client.enabled: true
            dns.client.discovery.enabled: false
            keystores.enabled: true
            jetty.server.enabled: false
            """;

    @Inject
    ClientBuilder clientBuilder;

    @Test
    public void testJwtLogin() throws Exception {
        System.setProperty("port", Integer.toString(Sockets.nextFreePort()));
        Configuration serverConfiguration = new ConfigurationBuilder().addTestDefaults().addYaml(config).addYaml(serverConfig).build();
        Configuration clientConfiguration = new ConfigurationBuilder().addTestDefaults().addYaml(config).addYaml(clientConfig).build();
        System.out.println(serverConfiguration);
        try (Xorcery server = new Xorcery(serverConfiguration)) {
            try (Xorcery client = new Xorcery(clientConfiguration)) {
                Logger logger = LogManager.getLogger();
                client.getServiceLocator().inject(this);

                InstanceConfiguration cfg = new InstanceConfiguration(serverConfiguration.getConfiguration("instance"));
                try (Client httpClient = clientBuilder
                        .register(JsonElementMessageBodyReader.class)
                        .register(JsonElementMessageBodyWriter.class)
                        .build()) {
                    JsonApiClient jsonApiClient = new JsonApiClient(httpClient);
                    jsonApiClient.get(new Link("login", cfg.getAPI().resolve("login").toASCIIString()))
                            .thenCompose(rd -> rd.getResource().map(ro ->
                                    jsonApiClient.submit(rd.getLinks().getSelf().orElseThrow(), new ResourceObject.Builder(ro)
                                            .attributes(new Attributes.Builder(ro.getAttributes().json())
                                                    .attribute("username", "someuser")
                                                    .attribute("password", "secret")
                                                    .build()).build())).orElseThrow(IllegalStateException::new))
                            .whenComplete((result, t) ->
                                    {
                                        String token = result.getAttributes().getString("token").orElseThrow();
                                        DecodedJWT decoded = JWT.decode(token);
                                        Assertions.assertEquals("someuser", decoded.getSubject());
                                        logger.info("alg:{},kid:{},typ:{},cty:{},claims:{}", decoded.getAlgorithm(), decoded.getKeyId(), decoded.getType(), decoded.getContentType(), decoded.getClaims());
                                    }
                            ).toCompletableFuture().join();
                }
            }
        }
    }
}
