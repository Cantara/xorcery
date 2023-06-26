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
package com.exoreaction.xorcery.dns.test.registration;

import com.exoreaction.xorcery.configuration.builder.StandardConfigurationBuilder;
import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.core.Xorcery;
import com.exoreaction.xorcery.dns.client.providers.DnsLookupService;
import com.exoreaction.xorcery.net.Sockets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;

//@Disabled
public class DnsRegistrationTest {

    String config = """
            jetty.client.enabled: true
            jetty.server.enabled: true
            jetty.server.http.enabled: true
            jetty.server.ssl.enabled: false

            dns.client.enabled: true
            dns.server.enabled: true
            dns.registration.enabled: true
            dns.dyndns.enabled: true
            dns.dyndns.key:
                name: xorcery.test
                secret: secret:BD077oHTdwm6Kwm4pc5tBkrX6EW3RErIOIESKpIKP6vQHAPRYp+9ubig Fvl3gYuuib+DQ8+eCpHEe/rIy9tiIg==
            """;

    @Test
    public void testRegisterServersAndServices() throws Exception {
        Logger logger = LogManager.getLogger(getClass());

        Configuration configuration1 = new Configuration.Builder()
                .with(new StandardConfigurationBuilder().addTestDefaultsWithYaml(config))
                .add("instance.id", "xorcery1")
                .add("instance.host", "server1")
                .add("jetty.server.http.port", Sockets.nextFreePort())
                .build();
        System.out.println(configuration1);
        Xorcery server = new Xorcery(configuration1);
        Xorcery client = new Xorcery(new Configuration.Builder()
                .with(new StandardConfigurationBuilder().addTestDefaultsWithYaml(config))
                .add("instance.id", "xorcery2")
                .add("instance.host", "server2")
                .add("dns.server.enabled", false)
                .add("jetty.server.http.port", Sockets.nextFreePort())
                .build());
        logger.info("After startup");
        DnsLookupService dnsLookupService = client.getServiceLocator().getService(DnsLookupService.class);
        {
            List<URI> hosts = dnsLookupService.resolve(URI.create("http://server1:80")).get();
            System.out.println(hosts);
        }
        {
            List<URI> hosts = dnsLookupService.resolve(URI.create("http://server2:80")).get();
            System.out.println(hosts);
        }

        {
            List<URI> hosts = dnsLookupService.resolve(URI.create("srv://_servicetest._sub._http._tcp.xorcery.test")).get();
            System.out.println(hosts);
        }

        client.close();
        logger.info("After client shutdown");
        dnsLookupService = server.getServiceLocator().getService(DnsLookupService.class);
        {
            List<URI> hosts = dnsLookupService.resolve(URI.create("http://server1:80")).get();
            System.out.println(hosts);
        }
        try
        {
            List<URI> hosts = dnsLookupService.resolve(URI.create("http://server2:80")).get();
            System.out.println(hosts);
        } catch (Throwable t)
        {
            logger.info("Could not look up server", t);
        }

        {
            List<URI> hosts = dnsLookupService.resolve(URI.create("srv://_servicetest._sub._http._tcp.xorcery.test")).get();
            System.out.println(hosts);
        }

        server.close();
    }
}
