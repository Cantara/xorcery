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
import com.exoreaction.xorcery.reactivestreams.api.server.ReactiveStreamsServer;
import com.exoreaction.xorcery.util.Resources;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CompletableFuture;

public class PersistentSubscribersRecoveryTest {

    static File tempRecoveryFile;

    static
    {
        try {
            tempRecoveryFile = File.createTempFile("recovery", ".yaml");
            tempRecoveryFile.deleteOnExit();

            byte[] template = Resources.getResource("testevents.yaml").orElseThrow().openStream().readAllBytes();
            Files.write(tempRecoveryFile.toPath(), template, StandardOpenOption.WRITE);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static String config = String.format("""
            jetty.server.http.port: %d
            jetty.server.ssl.enabled: false
            yamlfilepublisher:
                publishers:
                    - stream: "testevents"
                      file: "{{ instance.home }}/../test-classes/testevents.yaml"
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
            .build();
    private static CompletableFuture<Void> result;

    @Test
    public void testPersistentSubscriberRecovery() throws InterruptedException {

        while (TestSubscriber.handled.get() < 47)
        {
            System.out.println("SLEEP");
            Thread.sleep(1000);
        }
    }
}
