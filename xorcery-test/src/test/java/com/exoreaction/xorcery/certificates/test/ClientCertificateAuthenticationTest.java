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
package com.exoreaction.xorcery.certificates.test;

import com.exoreaction.xorcery.configuration.builder.StandardConfigurationBuilder;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.configuration.InstanceConfiguration;
import com.exoreaction.xorcery.core.Xorcery;
import com.exoreaction.xorcery.jsonapi.ResourceDocument;
import com.exoreaction.xorcery.test.ClientTester;
import com.exoreaction.xorcery.net.Sockets;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

public class ClientCertificateAuthenticationTest {

    String config = """
            dns.client.enabled: true
            dns.client.hosts:
                server1.xorcery.test: 127.0.0.1
                wrongserver.xorcery.test: 127.0.0.1
            dns.server.enabled: false
            dns.registration.enabled: false
            dns.server.registration.key:
                                name: xorcery.test
                                secret: BD077oHTdwm6Kwm4pc5tBkrX6EW3RErIOIESKpIKP6vQHAPRYp+9ubig Fvl3gYuuib+DQ8+eCpHEe/rIy9tiIg==
            jetty.client:
                enabled: true
                ssl:
                    enabled: true
            jetty.server.enabled: true
            jetty.server.ssl.enabled: true
            jetty.server.security.enabled: true    
            secrets.enabled: true
            keystores.enabled: true
            """;

    @Test
    public void testClientCertAuthValid() throws Exception {
        Logger logger = LogManager.getLogger(getClass());

        //System.setProperty("javax.net.debug", "ssl,handshake");

        int managerPort = Sockets.nextFreePort();
        managerPort = Sockets.nextFreePort();
        Configuration serverConfiguration = new Configuration.Builder()
                .with(new StandardConfigurationBuilder().addTestDefaultsWithYaml(config))
                .add("instance.id", "xorcery1")
                .add("instance.host", "server1")
                .add("jetty.server.http.port", Sockets.nextFreePort())
                .add("jetty.server.ssl.port", managerPort)
                .add("jetty.server.ssl.sniRequired", true)
                .add("jetty.server.security.enabled", true)
                .build();
//        System.out.println(serverConfiguration);
        Configuration clientConfiguration = new Configuration.Builder()
                .with(new StandardConfigurationBuilder().addTestDefaultsWithYaml(config))
                .add("instance.id", "xorcery2")
                .add("instance.host", "server2")
                .add("clienttester.enabled", "true")
                .add("jetty.server.enabled", false)
                .build();
        System.out.println(clientConfiguration);
        try (Xorcery server = new Xorcery(serverConfiguration)) {

            try (Xorcery client = new Xorcery(clientConfiguration)) {
                System.out.println("DONE");

                InstanceConfiguration cfg = new InstanceConfiguration(serverConfiguration.getConfiguration("instance"));
                ResourceDocument doc = client.getServiceLocator().getService(ClientTester.class).getResourceDocument(cfg.getURI().resolve("api/subject")).toCompletableFuture().join();
                Assertions.assertEquals(List.of("CN=Test Service"), doc.getResource().get().getAttributes().getListAs("principals", JsonNode::textValue).orElse(Collections.emptyList()));
            }

        }
    }
}
