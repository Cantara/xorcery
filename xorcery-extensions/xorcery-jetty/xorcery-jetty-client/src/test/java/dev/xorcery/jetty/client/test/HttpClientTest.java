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
package dev.xorcery.jetty.client.test;


import dev.xorcery.configuration.builder.ConfigurationBuilder;
import dev.xorcery.hk2.Instance;
import dev.xorcery.junit.XorceryExtension;
import org.eclipse.jetty.client.HttpClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class HttpClientTest {

    @RegisterExtension
    static XorceryExtension xorcery = XorceryExtension.xorcery()
            .configuration(ConfigurationBuilder::addTestDefaults)
            .addYaml("""
                    jetty.clients:
                               - name: "default"
                                 idleTimeout: 5s
                               - name: "custom"
                                 connectTimeout: 20s
                               - name: "http2"
                                 connectTimeout: 30s
                                 idleTimeout: 10s
                                 http2:
                                    enabled: true
                    """)
            .build();

    @Test
    public void testDefaultHttpClient(HttpClient httpClient){
        Assertions.assertEquals(5000, httpClient.getConnectTimeout());
        Assertions.assertEquals(5000, httpClient.getIdleTimeout());
    }

    @Test
    public void testCustomHttpClient(@Instance("custom") HttpClient httpClient){
        Assertions.assertEquals(20000, httpClient.getConnectTimeout());
        Assertions.assertEquals(5000, httpClient.getIdleTimeout());
    }

    @Test
    public void testHttp2Client(@Instance("http2") HttpClient httpClient){
        Assertions.assertEquals(30000, httpClient.getConnectTimeout());
        Assertions.assertEquals(10000, httpClient.getIdleTimeout());
    }
}
