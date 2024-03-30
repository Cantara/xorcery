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
package com.exoreaction.xorcery.test.certificates;

import com.exoreaction.xorcery.configuration.InstanceConfiguration;
import com.exoreaction.xorcery.configuration.builder.ConfigurationBuilder;
import com.exoreaction.xorcery.jsonapi.providers.JsonElementMessageBodyReader;
import com.exoreaction.xorcery.jsonapi.providers.JsonElementMessageBodyWriter;
import com.exoreaction.xorcery.junit.XorceryExtension;
import com.exoreaction.xorcery.net.Sockets;
import com.exoreaction.xorcery.test.ClientTester;
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
        //System.setProperty("javax.net.debug", "ssl,handshake");
    }

    @RegisterExtension
    @Order(1)
    static XorceryExtension caServer = XorceryExtension.xorcery()
            .id("xorcery1")
            .configuration(ConfigurationBuilder::addTestDefaults)
            .configuration(c -> c.addYaml(config).addYaml(String.format("""
                    certificates.server.enabled: true
                    intermediateca.enabled: true
                    jetty.server.ssl.port: %s
                    """, caPort)))
            .build();

    @RegisterExtension
    @Order(2)
    static XorceryExtension server = XorceryExtension.xorcery()
            .id("xorcery2")
            .configuration(ConfigurationBuilder::addTestDefaults)
            .configuration(c -> c.addYaml(config).addYaml(String.format("""
                    instance.id: xorcery2
                    instance.host: server2
                    jetty.client.enabled: true
                    certificates.enabled: true
                    certificates.renewOnStartup: true
                    certificates.client.enabled: true
                    keystores.ssl.path: "{{ instance.home }}/ssl.p12"
                    dns.client.hosts:
                        server.xorcery.test: 127.0.0.1
                        server2.xorcery.test: 127.0.0.1
                        _certificates._sub._https._tcp: "https://server.xorcery.test:%s/api"
                    jetty.server.ssl.needClientAuth: true
                    jetty.server.security:
                        type: CLIENT-CERT
                        constraints:
                            - name: clientcert
                        mappings:
                        - path: "/api/service"
                          constraint: clientcert
                    clienttester.enabled: true
                    """, caPort)))
            .build();


    @Test
    public void testCertificateRequestAndRenewal() throws Exception {

        System.out.println("Server config" + server.getConfiguration());

        URI service = InstanceConfiguration.get(server.getConfiguration()).getAPI().resolve("subject");
        System.out.println("Requesting:" + service);

        // Use Jetty client
        ContentResponse contentResponse = server.getServiceLocator().getService(HttpClient.class).newRequest(service).send();
        LogManager.getLogger().info(contentResponse.getContentAsString());
        Assertions.assertEquals(200, contentResponse.getStatus());

        // Use Jersey client
        Client client = server.getServiceLocator().getService(ClientBuilder.class)
                .register(JsonElementMessageBodyReader.class)
                .register(JsonElementMessageBodyWriter.class)
                .build();
        Response response = client.target(service).request().get();
        LogManager.getLogger().info(response.readEntity(String.class));
        Assertions.assertEquals(200, response.getStatus());
    }

    @Test
//    @RepeatedTest(1000)
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
