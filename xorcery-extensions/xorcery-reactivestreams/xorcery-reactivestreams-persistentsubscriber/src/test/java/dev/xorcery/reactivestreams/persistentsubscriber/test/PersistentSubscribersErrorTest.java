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
package dev.xorcery.reactivestreams.persistentsubscriber.test;

import dev.xorcery.configuration.builder.ConfigurationBuilder;
import dev.xorcery.junit.XorceryExtension;
import dev.xorcery.net.Sockets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class PersistentSubscribersErrorTest {

    static String config = String.format("""
            jetty.server.http.port: %d
            jetty.server.ssl.enabled: false
            persistentsubscribers:
                subscribers:
                    - name: testerrorsubscriber
                      uri: "{{ reactivestreams.server.uri }}"
                      stream: "testevents"
                      checkpoint: "{{ instance.home }}/testevents/checkpoint.yaml"
                      errors: "{{ instance.home }}/testevents/errors.yaml"
                      recovery: "{{ instance.home }}/testevents/recovery.yaml"
                      configuration:
                        environment: "{{ instance.environment }}"
                        """, Sockets.nextFreePort());

    @RegisterExtension
    static XorceryExtension xorcery = XorceryExtension.xorcery()
            .configuration(ConfigurationBuilder::addTestDefaults)
            .configuration(c -> c.addYaml(config))
            .with(new FilePublisher())
            .build();

    @Test
    public void testPersistentSubscriberErrors() throws InterruptedException {

        Thread.sleep(5000);
    }
}
