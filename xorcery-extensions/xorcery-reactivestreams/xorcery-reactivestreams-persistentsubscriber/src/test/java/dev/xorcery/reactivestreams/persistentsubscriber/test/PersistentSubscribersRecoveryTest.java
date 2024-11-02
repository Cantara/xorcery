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
import dev.xorcery.util.Resources;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

public class PersistentSubscribersRecoveryTest {

    static File tempRecoveryFile;

    static
    {
        try {
            tempRecoveryFile = File.createTempFile("recovery", ".yaml");
            tempRecoveryFile.deleteOnExit();

            byte[] template = Resources.getResource("persistentsubscribertestevents.yaml").orElseThrow().openStream().readAllBytes();
            Files.write(tempRecoveryFile.toPath(), template, StandardOpenOption.WRITE);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

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
                      recovery: "%s"
                      configuration:
                        environment: "{{ instance.environment }}"
                        """, Sockets.nextFreePort(), tempRecoveryFile.toString().replace('\\','/'));

    @RegisterExtension
    static XorceryExtension xorcery = XorceryExtension.xorcery()
            .configuration(ConfigurationBuilder::addTestDefaults)
            .configuration(c -> c.addYaml(config))
            .with(new FilePublisher())
            .build();

    @Test
    public void testPersistentSubscriberRecovery() throws InterruptedException {

        long start = System.currentTimeMillis();
        while (TestSubscriber.handled.get() < 47 && System.currentTimeMillis() < start + 10000)
        {
            System.out.println("SLEEP");
            Thread.sleep(1000);
        }

        if (System.currentTimeMillis() >= start + 10000)
            Assertions.fail("Timed out");
    }
}
