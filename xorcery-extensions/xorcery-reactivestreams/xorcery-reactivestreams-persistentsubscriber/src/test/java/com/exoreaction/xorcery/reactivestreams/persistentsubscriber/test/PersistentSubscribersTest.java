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
package com.exoreaction.xorcery.reactivestreams.persistentsubscriber.test;

import com.exoreaction.xorcery.configuration.builder.ConfigurationBuilder;
import com.exoreaction.xorcery.junit.XorceryExtension;
import com.exoreaction.xorcery.net.Sockets;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@Disabled
public class PersistentSubscribersTest {

    static String config = String.format("""
            jetty.server.http.port: %d
            jetty.server.ssl.enabled: false
            persistentsubscribers:
                subscribers:
                    - name: testsubscriber
                      uri: "{{ reactivestreams.server.uri }}"
                      stream: "testevents"
                      checkpoint: "{{ instance.home }}/testevents/checkpoint.yaml"
                      errors: "{{ instance.home }}/testevents/errors.yaml"
                      recovery: "{{ instance.home }}/testevents/recovery.yaml"

#                      uri: "ws://localhost:8889"
#                      stream: "events"
#                      checkpoint: "{{ SYSTEM.user_dir }}/testevents/checkpoint.yaml"
#                      errors: "{{ SYSTEM.user_dir }}/testevents/errors.yaml"
#                      recovery: "{{ SYSTEM.user_dir }}/testevents/recovery.yaml"

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
    public void testPersistentSubscriber() throws InterruptedException {

        long start = System.currentTimeMillis();
        while (TestSubscriber.handled.get() < 47 && System.currentTimeMillis() < start + 10000)
        {
//            System.out.println("SLEEP");
            Thread.sleep(100);
        }

        if (System.currentTimeMillis() >= start + 10000)
            Assertions.fail("Timed out");
    }

}
