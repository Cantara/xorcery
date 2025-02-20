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
package dev.xorcery.test.certificates;

import dev.xorcery.configuration.InstanceConfiguration;
import dev.xorcery.configuration.builder.ConfigurationBuilder;
import dev.xorcery.jsonapi.providers.JsonElementMessageBodyReader;
import dev.xorcery.jsonapi.providers.JsonElementMessageBodyWriter;
import dev.xorcery.junit.XorceryExtension;
import dev.xorcery.net.Sockets;
import dev.xorcery.test.ClientTester;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import org.apache.logging.log4j.LogManager;
import org.eclipse.jetty.client.ContentResponse;
import org.eclipse.jetty.client.HttpClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.net.URI;

public class CertificateRequestAndRenewalTest {

    static String caPort = Integer.toString(Sockets.nextFreePort());

    static String config = """
            jetty.server.enabled: true
            jetty.server.http.enabled: false
            jetty.server.ssl.enabled: true
            """;

    static {
        System.setProperty("javax.net.debug", "ssl,handshake");
        System.setProperty("port", caPort);
    }

    @RegisterExtension
    @Order(1)
    static XorceryExtension caServer = XorceryExtension.xorcery()
            .id("xorcery1")
            .configuration(ConfigurationBuilder::addTestDefaults)
            .configuration(c -> c.addResource("certificaterenewal/caserver.yaml"))
            .build();

    @RegisterExtension
    @Order(2)
    static XorceryExtension server = XorceryExtension.xorcery()
            .id("xorcery2")
            .configuration(ConfigurationBuilder::addTestDefaults)
            .configuration(c -> c.addResource("certificaterenewal/server1.yaml"))
            .build();


//    @RepeatedTest(1000)
    @Test
    public void testCertificateRequestAndRenewal() throws Exception {

        URI service = InstanceConfiguration.get(server.getConfiguration()).getAPI().resolve("subject");

        // Use Jetty client
        ContentResponse contentResponse = server.getServiceLocator().getService(HttpClient.class).newRequest(service).send();
        LogManager.getLogger().info(contentResponse.getContentAsString());
        Assertions.assertEquals(200, contentResponse.getStatus());

        // Use Jersey client
        try (Client client = server.getServiceLocator().getService(ClientBuilder.class)
                .register(JsonElementMessageBodyReader.class)
                .register(JsonElementMessageBodyWriter.class)
                .build())
        {
            Response response = client.target(service).request().get();
            LogManager.getLogger().info(response.readEntity(String.class));
            Assertions.assertEquals(200, response.getStatus());
        }
    }

    @Test
    public void testCRL() throws Exception {

        try {
            URI crlUri = InstanceConfiguration.get(caServer.getConfiguration()).getAPI().resolve("ca/intermediateca.crl");
            Response response = server.getServiceLocator().getService(ClientTester.class).get(UriBuilder.fromUri(crlUri).build()).get();
            Assertions.assertEquals(200, response.getStatus());
            System.out.println(response.readEntity(String.class));
        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
        }
    }
}
