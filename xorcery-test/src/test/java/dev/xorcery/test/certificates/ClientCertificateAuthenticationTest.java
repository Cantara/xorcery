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

import com.fasterxml.jackson.databind.JsonNode;
import dev.xorcery.configuration.Configuration;
import dev.xorcery.configuration.InstanceConfiguration;
import dev.xorcery.configuration.builder.ConfigurationBuilder;
import dev.xorcery.core.Xorcery;
import dev.xorcery.jsonapi.ResourceDocument;
import dev.xorcery.net.Sockets;
import dev.xorcery.test.ClientTester;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

@Disabled("need to fix this")
public class ClientCertificateAuthenticationTest {

    String config = """
            dns.server.enabled: false
            dns.registration.enabled: false
            dns.server.registration.key:
                                name: xorcery.test
                                secret: BD077oHTdwm6Kwm4pc5tBkrX6EW3RErIOIESKpIKP6vQHAPRYp+9ubig Fvl3gYuuib+DQ8+eCpHEe/rIy9tiIg==
                    """;

    String serverConfig = """
            instance.id: "xorcery1"
            instance.host: "server"
            jetty.server.enabled: true
            jetty.server.security.type: CLIENT-CERT    
            jetty.server.ssl.port: "{{SYSTEM.port}}"
            jetty.server.ssl.sniRequired: true
            jetty.server.security.enabled: true
                    """;

    String clientConfig = """
            dns.client.hosts:
                - name: server.xorcery.test
                  url: "127.0.0.1"
                - name: wrongserver.xorcery.test
                  url: "127.0.0.1"
                  
            clienttester.enabled: true
            jetty.client:
                enabled: true
                ssl:
                    enabled: true
                    """;

    @Test
    public void testClientCertAuthValid() throws Exception {
        System.setProperty("javax.net.debug", "ssl,handshake");

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
                ResourceDocument doc = client.getServiceLocator().getService(ClientTester.class).getResourceDocument(cfg.getAPI().resolve("subject")).toCompletableFuture().join();
                Assertions.assertEquals(List.of("CN=Test Service", "localhost", "server.xorcery.test", "127.0.0.1"), doc.getResource().get().getAttributes().getListAs("names", JsonNode::textValue).orElse(Collections.emptyList()));
            }
        }
    }
}
