package com.exoreaction.xorcery.service.certificates.test;

import com.exoreaction.xorcery.configuration.builder.StandardConfigurationBuilder;
import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.core.Xorcery;
import com.exoreaction.xorcery.util.Sockets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled
public class CertificateRequestAndRenewalTest {

    String config = """
            hk2.threadpolicy: USE_NO_THREADS
            dns.hosts:
                .server1.xorcery.test: 127.0.0.1
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
            certificates.enabled: true
            """;

    @Test
    public void testCertificateRequestAndRenewal() throws Exception {
        Logger logger = LogManager.getLogger(getClass());

        //System.setProperty("javax.net.debug", "ssl,handshake");

        int managerPort = Sockets.nextFreePort();
        managerPort = 443;
        Configuration configuration1 = new Configuration.Builder()
                .with(new StandardConfigurationBuilder().addTestDefaultsWithYaml(config))
                .add("id", "xorcery1")
                .add("host", "server1.xorcery.test")
                .add("server.http.port", 8080)
                .add("server.ssl.port", managerPort)
                .add("certificates.keystore.path", "META-INF/intermediatecakeystore.p12")
                .add("certificates.truststore.path", "META-INF/intermediatecatruststore.p12")
                .add("certificates.server.enabled", true)
                .add("certificates.server.self.enabled", true)
                .build();
        System.out.println(StandardConfigurationBuilder.toYaml(configuration1));
        Configuration configuration2 = new Configuration.Builder()
                .with(new StandardConfigurationBuilder().addTestDefaultsWithYaml(config))
                .add("id", "xorcery2")
                .add("host", "server2.xorcery.test")
                .add("server.http.port", Sockets.nextFreePort())
                .add("server.ssl.port", 8443)
                .add("certificates.keystore.path", "META-INF/keystore.p12")
                .add("certificates.truststore.path", "META-INF/truststore.p12")
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
}
