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
package dev.xorcery.admin.servlet;

import dev.xorcery.configuration.Configuration;
import dev.xorcery.configuration.InstanceConfiguration;
import dev.xorcery.configuration.builder.ConfigurationBuilder;
import dev.xorcery.core.Xorcery;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.MediaType;
import org.glassfish.hk2.api.ServiceLocator;
import org.junit.jupiter.api.Test;

public class AdminTest {

    @Test
    void testHealth() throws Exception {
        Configuration configuration = new ConfigurationBuilder().addTestDefaults()
                .addYaml("""
                        jetty.server.http.port: "{{ CALCULATED.dynamicPorts.http }}"
                        jetty.server.ssl.enabled: false
                        jety.client.ssl.enabled: false
                        """).build();

        Xorcery xorcery = new Xorcery(configuration);
        ServiceLocator serviceLocator = xorcery.getServiceLocator();

        Client client = serviceLocator.getService(ClientBuilder.class).build();

        String response = client.target(InstanceConfiguration.get(configuration).getURI())
                .path("/health")
                .queryParam("compact", "true")
                .request(MediaType.APPLICATION_JSON_TYPE)
                        .get(String.class);

//        System.out.println(response);
    }
}
