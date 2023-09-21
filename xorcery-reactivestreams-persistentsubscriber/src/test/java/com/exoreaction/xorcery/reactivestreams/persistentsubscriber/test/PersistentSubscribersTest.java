package com.exoreaction.xorcery.reactivestreams.persistentsubscriber.test;

import com.exoreaction.xorcery.configuration.builder.ConfigurationBuilder;
import com.exoreaction.xorcery.junit.XorceryExtension;
import com.exoreaction.xorcery.net.Sockets;
import com.exoreaction.xorcery.reactivestreams.api.server.ReactiveStreamsServer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.concurrent.CompletableFuture;

public class PersistentSubscribersTest {

    static String config = String.format("""
            jetty.server.http.port: %d
            jetty.server.ssl.enabled: false
            persistentsubscribers:
                subscribers:
                    - name: testsubscriber
                      uri: "{{ reactivestreams.server.uri }}"
                      stream: "testevents"
                      checkpointProvider: "file"
                      errorLogProvider: "file"
#                      uri: "ws://localhost:8889"
#                      stream: "events"
#                      checkpoint: "{{ instance.home }}/testevents/checkpoint.yaml"
                      errors: "{{ instance.home }}/testevents/errors.yaml"
#                      recovery: "{{ instance.home }}/testevents/recovery.yaml"

                      checkpoint: "{{ SYSTEM.user_dir }}/testevents/checkpoint.yaml"
                      errors: "{{ SYSTEM.user_dir }}/testevents/errors.yaml"
                      recovery: "{{ SYSTEM.user_dir }}/testevents/recovery.yaml"
                      configuration:
                        environment: "{{ instance.environment }}"
                        """, Sockets.nextFreePort());

    @RegisterExtension
    static XorceryExtension xorcery = XorceryExtension.xorcery()
            .configuration(ConfigurationBuilder::addTestDefaults)
            .configuration(c -> c.addYaml(config))
            .build();
    private static CompletableFuture<Void> result;

    @BeforeAll
    public static void setup()
    {
        YamlFilePublisher yamlFilePublisher = new YamlFilePublisher(PersistentSubscribersTest.class.getResource("/testevents.yaml"));
        result = xorcery.getXorcery().getServiceLocator().getService(ReactiveStreamsServer.class).publisher("testevents", cfg -> yamlFilePublisher, YamlFilePublisher.class);
    }

    @Test
    public void testPersistentSubscriber() throws InterruptedException {
        while (TestSubscriber.handled.get() < 47)
        {
            System.out.println("SLEEP");
            Thread.sleep(100);
        }
    }
}
