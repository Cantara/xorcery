package com.exoreaction.xorcery.service.certificates.test;

import com.exoreaction.xorcery.configuration.builder.StandardConfigurationBuilder;
import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.configuration.model.StandardConfiguration;
import com.exoreaction.xorcery.core.Xorcery;
import com.exoreaction.xorcery.service.ClientTester;
import com.exoreaction.xorcery.util.Sockets;
import jakarta.ws.rs.core.UriBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.net.URI;

@Disabled
public class CertificateRequestAndRenewalTest {

    String config = """
            dns.client.hosts:
                .server1.xorcery.test: 127.0.0.1
            dns.server.enabled: false
            dns.server.discovery.enabled: false
            dns.server.multicast.enabled: false
            dns.server.registration.enabled: false
            dns.server.registration.key:
                                name: xorcery.test
                                secret: BD077oHTdwm6Kwm4pc5tBkrX6EW3RErIOIESKpIKP6vQHAPRYp+9ubig Fvl3gYuuib+DQ8+eCpHEe/rIy9tiIg==
            jetty.client:
                enabled: true
                ssl:
                    enabled: true
            jetty.server.enabled: true
            jetty.server.ssl.enabled: true    
            keystores.enabled: true
            """;

    @Test
    public void testCertificateRequestAndRenewal() throws Exception {
        Logger logger = LogManager.getLogger(getClass());

        //System.setProperty("javax.net.debug", "ssl,handshake");

        int managerPort = Sockets.nextFreePort();
        managerPort = 8443;
        Configuration configuration1 = new Configuration.Builder()
                .with(new StandardConfigurationBuilder().addTestDefaultsWithYaml(config))
                .add("id", "xorcery1")
                .add("host", "server1.xorcery.test")
                .add("jetty.server.http.port", Sockets.nextFreePort())
                .add("jetty.server.ssl.port", managerPort)
                .add("keystores.keystore.path", "META-INF/intermediatecakeystore.p12")
                .add("keystores.truststore.path", "META-INF/intermediatecatruststore.p12")
                .add("certificates.server.enabled", true)
                .add("certificates.server.self.enabled", true)
                .build();
        System.out.println(StandardConfigurationBuilder.toYaml(configuration1));
        Configuration configuration2 = new Configuration.Builder()
                .with(new StandardConfigurationBuilder().addTestDefaultsWithYaml(config))
                .add("id", "xorcery2")
                .add("host", "server2.xorcery.test")
                .add("jetty.server.http.port", Sockets.nextFreePort())
                .add("jetty.server.ssl.port", Sockets.nextFreePort())
                .add("keystores.keystore.path", "META-INF/keystore.p12")
                .add("keystores.truststore.path", "META-INF/truststore.p12")
                .add("certificates.client.enabled", true)
                .add("certificates.client.renewonstartup", true)
                .add("certificates.client.host", "https://192.168.1.107:" + managerPort)
                .build();
//        System.out.println(StandardConfigurationBuilder.toYaml(configuration2));
        try (Xorcery xorcery1 = new Xorcery(configuration1)) {
            try (Xorcery xorcery2 = new Xorcery(configuration2)) {
                System.out.println("DONE");
//                Thread.sleep(5000000);
            }
        }
    }

    @Test
    public void testCRL() throws Exception {
        Logger logger = LogManager.getLogger(getClass());

        //System.setProperty("javax.net.debug", "ssl,handshake");

        int managerPort = Sockets.nextFreePort();
        managerPort = 8443;
        Configuration serverConfiguration = new Configuration.Builder()
                .with(new StandardConfigurationBuilder().addTestDefaultsWithYaml(config))
                .add("id", "xorcery1")
                .add("host", "server.xorcery.test")
                .add("jetty.server.http.port", Sockets.nextFreePort())
                .add("jetty.server.ssl.port", managerPort)
//                .add("server.ssl.enabled", false)
                .add("keystores.keystore.path", "META-INF/intermediatecakeystore.p12")
//                .add("keystores.keystore.path", "../xorcery-certificates-server/rootcakeystore.p12")
//                .add("certificates.server.alias", "root")
                .add("keystores.truststore.path", "META-INF/intermediatecatruststore.p12")
                .add("certificates.server.enabled", true)
                .add("certificates.server.self.enabled", true)
                .build();
        System.out.println(StandardConfigurationBuilder.toYaml(serverConfiguration));
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
                URI crlUri = cfg.getServerUri().resolve("api/certificates/crl");
                InputStream input = (InputStream) client.getServiceLocator().getService(ClientTester.class).get(UriBuilder.fromUri(crlUri).build()).get().getEntity();
                System.out.println(new String(input.readAllBytes()));
//                CRL crl = CertificateFactory.getInstance("X.509").generateCRL(input);
//                System.out.println(crl.toString());
//                Thread.sleep(5000000);
            }

        }
    }
}
