package com.exoreaction.xorcery.service.dns.discovery;

import com.exoreaction.xorcery.configuration.builder.StandardConfigurationBuilder;
import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.core.Xorcery;
import com.exoreaction.xorcery.service.dns.client.DnsLookupService;
import com.exoreaction.xorcery.util.Sockets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.fail;

@Disabled
public class DnsDiscoveryTest {

    String config = """
            dns.server.enabled: false
            dns.server.discovery.enabled: true
            dns.server.multicast.enabled: true
            dns.server.registration.enabled: true
            dns.server.registration.key:
                                name: xorcery.test
                                secret: BD077oHTdwm6Kwm4pc5tBkrX6EW3RErIOIESKpIKP6vQHAPRYp+9ubig Fvl3gYuuib+DQ8+eCpHEe/rIy9tiIg==             
            client:
                enabled: true
            server.enabled: true    
            """;

    @Test
    public void testDiscovery() throws Exception {
        Logger logger = LogManager.getLogger(getClass());


        Configuration configuration1 = new Configuration.Builder()
                .with(new StandardConfigurationBuilder().addTestDefaultsWithYaml(config))
                .add("id", "xorcery1")
                .add("host", "server1.xorcery.test")
                .add("jetty.server.http.port", Sockets.nextFreePort())
                .build();
        System.out.println(configuration1.toJsonString());
        try (Xorcery xorcery1 = new Xorcery(configuration1)) {
            try (Xorcery xorcery2 = new Xorcery(new Configuration.Builder()
                    .with(new StandardConfigurationBuilder().addTestDefaultsWithYaml(config))
                    .add("id", "xorcery2")
                    .add("host", "server2.xorcery.test")
                    .add("jetty.server.http.port", Sockets.nextFreePort())
                    .build())) {
                logger.info("After startup");
                DnsLookupService dnsLookupService = xorcery2.getServiceLocator().getService(DnsLookupService.class);
                {
                    List<URI> hosts = dnsLookupService.resolve(URI.create("srv://_http._tcp.xorcery.test")).get();
                    LogManager.getLogger(getClass()).info("Dynamic lookup:"+hosts);
                }
            }
        }

/*
        // Do this to get fresh DNS cache
        DnsLookupService dnsLookupService = new DnsLookupService(new Configuration.Builder()
                .add("dns.search", List.of("xorcery.test."))
                .build());

        try {
            logger.info("After shutdown");
            {
                List<URI> hosts = dnsLookupService.resolve(URI.create("http://server1.xorcery.test:80")).get();
                System.out.println(hosts);
            }
            fail();
        } catch (ExecutionException e) {
            // Correct
        }
        try {
            {
                List<URI> hosts = dnsLookupService.resolve(URI.create("srv://_servicetest._tcp.xorcery.test")).get();
                System.out.println(hosts);
            }
            fail();
        } catch (ExecutionException e) {
            // Correct
        }
*/
    }}
