package com.exoreaction.xorcery.service.certificates.test;

import com.exoreaction.xorcery.configuration.builder.StandardConfigurationBuilder;
import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.configuration.model.StandardConfiguration;
import com.exoreaction.xorcery.core.Xorcery;
import com.exoreaction.xorcery.jsonapi.model.ResourceDocument;
import com.exoreaction.xorcery.service.ClientTester;
import com.exoreaction.xorcery.util.Sockets;
import jakarta.ws.rs.BadRequestException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ClientCertificateAuthenticationTest {

    String config = """
            hk2.threadpolicy: USE_NO_THREADS
            dns.client.hosts:
                .server1.xorcery.test: 127.0.0.1
                .wrongserver.xorcery.test: 127.0.0.1
            dns.server.enabled: false
            dns.server.discovery.enabled: false
            dns.server.multicast.enabled: false
            dns.server.registration.enabled: false
            dns.server.registration.key:
                                name: xorcery.test
                                secret: BD077oHTdwm6Kwm4pc5tBkrX6EW3RErIOIESKpIKP6vQHAPRYp+9ubig Fvl3gYuuib+DQ8+eCpHEe/rIy9tiIg==
            client:
                enabled: true
                ssl:
                    enabled: true
            server.enabled: true
            server.ssl.enabled: true    
            keystores.enabled: true
            """;

    @Test
    public void testSniCheckValid() throws Exception {
        Logger logger = LogManager.getLogger(getClass());

        //System.setProperty("javax.net.debug", "ssl,handshake");

        int managerPort = Sockets.nextFreePort();
        managerPort = 8443;
        Configuration serverConfiguration = new Configuration.Builder()
                .with(new StandardConfigurationBuilder().addTestDefaultsWithYaml(config))
                .add("id", "xorcery1")
                .add("host", "server.xorcery.test")
                .add("server.http.port", Sockets.nextFreePort())
                .add("server.ssl.port", managerPort)
                .add("server.ssl.snirequired", true)
                .add("server.security.enabled", true)
                .build();
//        System.out.println(StandardConfigurationBuilder.toYaml(serverConfiguration));
        Configuration clientConfiguration = new Configuration.Builder()
                .with(new StandardConfigurationBuilder().addTestDefaultsWithYaml(config))
                .add("id", "xorcery2")
                .add("host", "server2.xorcery.test")
                .add("clienttester.enabled", "true")
                .add("server.enabled", false)
                .add("server.enabled", false)
                .build();
        System.out.println(StandardConfigurationBuilder.toYaml(clientConfiguration));
        try (Xorcery server = new Xorcery(serverConfiguration)) {

            try (Xorcery client = new Xorcery(clientConfiguration)) {
                System.out.println("DONE");

                StandardConfiguration cfg = () -> serverConfiguration;
                ResourceDocument doc = client.getServiceLocator().getService(ClientTester.class).getResourceDocument(cfg.getServerUri()).toCompletableFuture().join();
                ResourceDocument doc2 = client.getServiceLocator().getService(ClientTester.class).getResourceDocument(cfg.getServerUri()).toCompletableFuture().join();
                ResourceDocument doc3 = client.getServiceLocator().getService(ClientTester.class).getResourceDocument(cfg.getServerUri()).toCompletableFuture().join();
//                Thread.sleep(5000000);
            }

        }
    }

    @Test
    public void testClientCertAuthInvalid() throws Exception {
        Logger logger = LogManager.getLogger(getClass());

        //System.setProperty("javax.net.debug", "ssl,handshake");

        int managerPort = Sockets.nextFreePort();
        managerPort = 8443;
        Configuration serverConfiguration = new Configuration.Builder()
                .with(new StandardConfigurationBuilder().addTestDefaultsWithYaml(config))
                .add("id", "xorcery1")
                .add("host", "server.xorcery.test")
                .add("server.http.port", Sockets.nextFreePort())
                .add("server.ssl.port", managerPort)
                .add("server.ssl.snirequired", true)
                .add("keystores.truststore.path", "META-INF/intermediatecatruststore.p12")
                .build();
//        System.out.println(StandardConfigurationBuilder.toYaml(serverConfiguration));
        Configuration clientConfiguration = new Configuration.Builder()
                .with(new StandardConfigurationBuilder().addTestDefaultsWithYaml(config))
                .add("id", "xorcery2")
                .add("host", "server2.xorcery.test")
                .add("clienttester.enabled", "true")
                .add("server.enabled", false)
                .add("server.enabled", false)
                .build();
        System.out.println(StandardConfigurationBuilder.toYaml(clientConfiguration));
        try (Xorcery server = new Xorcery(serverConfiguration)) {

            try (Xorcery client = new Xorcery(clientConfiguration)) {
                System.out.println("DONE");

                Assertions.assertThrows(BadRequestException.class, ()->
                {
                    StandardConfiguration cfg = () -> serverConfiguration;
                    ResourceDocument doc = client.getServiceLocator().getService(ClientTester.class).getResourceDocument(cfg.getServerUri()).toCompletableFuture().join();
                });
            }

        }
    }


}
