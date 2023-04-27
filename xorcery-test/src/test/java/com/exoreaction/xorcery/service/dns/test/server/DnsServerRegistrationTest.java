package com.exoreaction.xorcery.service.dns.test.server;

import com.exoreaction.xorcery.configuration.builder.StandardConfigurationBuilder;
import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.core.Xorcery;
import com.exoreaction.xorcery.service.dns.client.DnsLookupService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;

public class DnsServerRegistrationTest {

    String serverConfig = """
            jetty.client.enabled: false
            jetty.server.enabled: false
            dns.server:
                enabled: true
                keys:
                    - name: updatekey
                      secret: BD077oHTdwm6Kwm4pc5tBkrX6EW3RErIOIESKpIKP6vQHAPRYp+9ubig Fvl3gYuuib+DQ8+eCpHEe/rIy9tiIg==
                zones:
                    - name: xorcery.test
                      allow-update:
                        - key: updatekey 
            """;

    String clientConfig = """
            jetty.client.enabled: false
            jetty.server.enabled: false
            dns.client.enabled: true
            dns.client.discovery.enabled: false
            dns.client.nameServers:
                - 127.0.0.1:8853
            dns.registration:
                enabled: true
                announce:
                    enabled: false
                key:
                    name: updatekey
                    secret: BD077oHTdwm6Kwm4pc5tBkrX6EW3RErIOIESKpIKP6vQHAPRYp+9ubig Fvl3gYuuib+DQ8+eCpHEe/rIy9tiIg==             
            """;

    @Test
    public void testRegisterOnServer() throws Exception {
        Logger logger = LogManager.getLogger(getClass());

        Configuration serverConfiguration = new Configuration.Builder()
                .with(new StandardConfigurationBuilder().addTestDefaultsWithYaml(serverConfig))
                .build();
        System.out.println(serverConfiguration.toJsonString());
        try (Xorcery xorcery1 = new Xorcery(serverConfiguration)) {
            try (Xorcery xorcery2 = new Xorcery(new Configuration.Builder()
                    .with(new StandardConfigurationBuilder().addTestDefaultsWithYaml(clientConfig))
                    .build())) {
                logger.info("After startup");
                DnsLookupService dnsLookupService = xorcery2.getServiceLocator().getService(DnsLookupService.class);
/*
                {
                    List<URI> hosts = dnsLookupService.resolve(URI.create("http://server.xorcery.test:80")).get();
                    System.out.println(hosts);
                }
                {
                    List<URI> hosts = dnsLookupService.resolve(URI.create("http://server:80")).get();
                    System.out.println(hosts);
                }
*/

/*
                {
                    List<URI> hosts = dnsLookupService.resolve(URI.create("srv://_servicetest._sub._http._tcp.xorcery.test")).get();
                    System.out.println(hosts);
                }
*/

                {
                    List<URI> hosts = dnsLookupService.resolve(URI.create("srv://_servicetest._sub._http._tcp")).get();
                    System.out.println(hosts);
                }
            }
        }
    }
}
