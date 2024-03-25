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
package com.exoreaction.xorcery.test.dns;

import com.exoreaction.xorcery.configuration.builder.ConfigurationBuilder;
import com.exoreaction.xorcery.configuration.builder.StandardConfigurationBuilder;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.core.Xorcery;
import com.exoreaction.xorcery.jsonapi.MediaTypes;
import com.exoreaction.xorcery.jsonapi.providers.JsonElementMessageBodyReader;
import com.exoreaction.xorcery.jsonapi.ResourceDocument;
import com.exoreaction.xorcery.net.Sockets;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.client.ClientConfig;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class SRVConnectorTest {

    private final String config = """
            jetty.server.enabled: true
            jetty.server.http.enabled: false
            jetty.server.ssl.port: "{{ SYSTEM.port }}"
            dns.registration.enabled: true            
            dns.dyndns.enabled: true
            """;

    @Test
    public void testClientSRVConnectorLoadBalancing() throws Exception {
        System.setProperty("port", Integer.toString(Sockets.nextFreePort()));
        Configuration server1Configuration = new ConfigurationBuilder().addTestDefaults().addYaml(config).addYaml("""                        
                        instance.host: server1
                        servicetest.srv.weight: 50
                        dns:
                            server:
                                enabled: true
                        """).build();
        System.setProperty("port", Integer.toString(Sockets.nextFreePort()));
        Configuration server2Configuration = new ConfigurationBuilder().addTestDefaults().addYaml(config).addYaml("""
                        instance.host: server2
                        jetty.client.enabled: true
                        jetty.client.ssl.trustAll: true
                        """).build();

        try (Xorcery server1 = new Xorcery(server1Configuration)) {
            try (Xorcery server2 = new Xorcery(server2Configuration)) {
//                LogManager.getLogger().info("Server 1 configuration:\n" + server1Configuration);
//                LogManager.getLogger().info("Server 2 configuration:\n" + server2Configuration);
                ClientBuilder clientConfig = server2.getServiceLocator().getService(ClientBuilder.class);
                try (Client httpClient = clientConfig.register(JsonElementMessageBodyReader.class).build())
                {
                    for (int i = 0; i < 10; i++) {
                        ResourceDocument resourceDocument = httpClient.target("srv://_servicetest._sub._https._tcp/")
                                .request(MediaTypes.APPLICATION_JSON_API).get().readEntity(ResourceDocument.class);
                        System.out.println(resourceDocument.getLinks().getByRel("self").orElse(null).getHrefAsUri());
                        System.out.println(resourceDocument.getResource().get().getAttributes().toMap());
                    }
                }
            }
        }
    }

    @Test
    @Disabled
    public void testClientSRVResolverFailover() throws Exception {
        StandardConfigurationBuilder standardConfigurationBuilder = new StandardConfigurationBuilder();
        Configuration configuration2 = new Configuration.Builder()
                .with(standardConfigurationBuilder.addTestDefaultsWithYaml("""
                        name: xorcery1
                        jetty.server.http.port: 8888
                        dns:
                            enabled: true
                            hosts:
                                analytics:
                                    - 127.0.0.1:8888
                                    - 127.0.0.1:8080
                            server:
                                enabled: true
                        """)).build();
//        logger.info("Resolved configuration2:\n" + standardConfigurationBuilder.toYaml(configuration2));

        try (Xorcery xorcery2 = new Xorcery(configuration2)) {
            ClientConfig clientConfig = xorcery2.getServiceLocator().getService(ClientConfig.class);
            Client client = ClientBuilder.newClient(clientConfig
                    .register(JsonElementMessageBodyReader.class)
            );

            for (int i = 0; i < 100; i++) {
                ResourceDocument resourceDocument = client.target("http://analytics").request().get().readEntity(ResourceDocument.class);
                System.out.println(resourceDocument.getLinks().getByRel("self").orElse(null).getHrefAsUri());
                System.out.println(resourceDocument.getMeta().getMeta().toPrettyString());
//                    MultivaluedMap<String, Object> headers = client.target("http://analytics").request().get().getHeaders();
//                    System.out.println(headers);
//                    Thread.sleep(1000);
            }
        }
    }
}
