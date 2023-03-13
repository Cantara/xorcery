package com.exoreaction.xorcery.service.certificates.test;

import com.exoreaction.xorcery.configuration.builder.StandardConfigurationBuilder;
import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.configuration.model.InstanceConfiguration;
import com.exoreaction.xorcery.core.Xorcery;
import com.exoreaction.xorcery.jsonapi.model.ResourceDocument;
import com.exoreaction.xorcery.service.ClientTester;
import com.exoreaction.xorcery.util.Sockets;
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
                .server1.xorcery.test: 127.0.0.1
                .wrongserver.xorcery.test: 127.0.0.1
            dns.server.enabled: false
            dns.discovery.enabled: false
            dns.multicast.enabled: false
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
                .add("instance.host", "server")
                .add("jetty.server.http.port", Sockets.nextFreePort())
                .add("jetty.server.ssl.port", managerPort)
                .add("jetty.server.ssl.snirequired", true)
                .add("jetty.server.security.enabled", true)
                .build();
//        System.out.println(StandardConfigurationBuilder.toYaml(serverConfiguration));
        Configuration clientConfiguration = new Configuration.Builder()
                .with(new StandardConfigurationBuilder().addTestDefaultsWithYaml(config))
                .add("instance.id", "xorcery2")
                .add("instance.host", "server2")
                .add("clienttester.enabled", "true")
                .add("jetty.server.enabled", false)
                .build();
        System.out.println(StandardConfigurationBuilder.toYaml(clientConfiguration));
        try (Xorcery server = new Xorcery(serverConfiguration)) {

            try (Xorcery client = new Xorcery(clientConfiguration)) {
                System.out.println("DONE");

                InstanceConfiguration cfg = new InstanceConfiguration(serverConfiguration.getConfiguration("instance"));
                ResourceDocument doc = client.getServiceLocator().getService(ClientTester.class).getResourceDocument(cfg.getServerUri().resolve("api/subject")).toCompletableFuture().join();
                Assertions.assertEquals(List.of("CN=Test Service"), doc.getResource().get().getAttributes().getListAs("principals", JsonNode::textValue).orElse(Collections.emptyList()));
            }

        }
    }
}
