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

import com.exoreaction.xorcery.configuration.builder.ConfigurationBuilder;
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
            dns.client.hosts:
                server.xorcery.test: 127.0.0.1
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
            jetty.server.http.enabled: false
            jetty.server.ssl.enabled: true
            jetty.server.security.enabled: true    
            jetty.server.security.method: CLIENT_CERT    
                    """;

    String serverConfig = """
            instance.id: "xorcery1"
            instance.host: "server"
            jetty.server.ssl.port: "{{SYSTEM.port}}"
            jetty.server.ssl.sniRequired: true
            jetty.server.security.enabled: true
                    """;

    String clientConfig = """
            jetty.server.enabled: false
            reactivestreams.server.enabled: false
            clienttester.enabled: true
                    """;

    @Test
    public void testClientCertAuthValid() throws Exception {
        //System.setProperty("javax.net.debug", "ssl,handshake");

        System.setProperty("port", Integer.toString(Sockets.nextFreePort()));
        System.out.println(System.getProperty("port"));
        Configuration serverConfiguration = new ConfigurationBuilder()
                .addTestDefaults()
                .addYaml(config)
                .addYaml(serverConfig)
                .build();
        System.out.println(serverConfiguration);
        Configuration clientConfiguration = new ConfigurationBuilder()
                .addTestDefaults()
                .addYaml(config)
                .addYaml(clientConfig)
                .build();
//        System.out.println(clientConfiguration);
        try (Xorcery server = new Xorcery(serverConfiguration)) {
            try (Xorcery client = new Xorcery(clientConfiguration)) {
                InstanceConfiguration cfg = new InstanceConfiguration(serverConfiguration.getConfiguration("instance"));
                ResourceDocument doc = client.getServiceLocator().getService(ClientTester.class).getResourceDocument(cfg.getURI().resolve("api/subject")).toCompletableFuture().join();
                Assertions.assertEquals(List.of("CN=Test Service", "localhost", "server.xorcery.test", "127.0.0.1"), doc.getResource().get().getAttributes().getListAs("names", JsonNode::textValue).orElse(Collections.emptyList()));
            }
        }
    }
}
