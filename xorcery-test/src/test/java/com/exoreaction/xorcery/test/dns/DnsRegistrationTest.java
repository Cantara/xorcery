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
package com.exoreaction.xorcery.test.dns;

import com.exoreaction.xorcery.configuration.builder.ConfigurationBuilder;
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
import java.util.concurrent.TimeUnit;

//@Disabled
public class DnsRegistrationTest {

    String config = """
            jetty.client.enabled: true
            jetty.server.enabled: true
            jetty.server.http.enabled: true
            jetty.server.ssl.enabled: false

            dns.client.enabled: true
            dns.client.forceTcp: true
            dns.registration.enabled: true
            dns.dyndns.enabled: true
            dns.dyndns.key:
                name: xorcery.test
                secret: secret:BD077oHTdwm6Kwm4pc5tBkrX6EW3RErIOIESKpIKP6vQHAPRYp+9ubig Fvl3gYuuib+DQ8+eCpHEe/rIy9tiIg==
            """;

    String server1 = """
            instance.id: "xorcery1"
            instance.host: "server1"
            dns.server.enabled: true
            jetty.server.http.port: "{{ SYSTEM.server1 }}"
                    """;

    String server2 = """
            instance.id: "xorcery2"
            instance.host: "server2"
            jetty.server.http.port: "{{ SYSTEM.server2 }}"
            """;

    @Test
    public void testRegisterServersAndServices() throws Exception {
        Logger logger = LogManager.getLogger(getClass());

        System.setProperty("server1", Integer.toString(Sockets.nextFreePort()));
        System.setProperty("server2", Integer.toString(Sockets.nextFreePort()));
        Configuration server1Config = new ConfigurationBuilder().addTestDefaults().addYaml(config).addYaml(server1).build();
        Configuration server2Config = new ConfigurationBuilder().addTestDefaults().addYaml(config).addYaml(server2).build();
        System.out.println("server1:\n"+server1Config);
        System.out.println("server2:\n"+server2Config);
        Xorcery xorcery1 = new Xorcery(server1Config);
        Xorcery xorcery2 = new Xorcery(server2Config);
        logger.info("After startup");
        DnsLookupService dnsLookupService = xorcery2.getServiceLocator().getService(DnsLookupService.class);
        {
            List<URI> hosts = dnsLookupService.resolve(URI.create("http://server1:80")).orTimeout(10, TimeUnit.SECONDS).join();
            System.out.println("http://server1:80 "+hosts);
        }
        {
            List<URI> hosts = dnsLookupService.resolve(URI.create("http://server2:80")).orTimeout(10, TimeUnit.SECONDS).join();
            System.out.println("http://server2:80 "+hosts);
        }

        {
            List<URI> hosts = dnsLookupService.resolve(URI.create("srv://_servicetest._sub._http._tcp.xorcery.test")).orTimeout(10, TimeUnit.SECONDS).join();
            System.out.println("srv://_servicetest._sub._http._tcp.xorcery.test "+hosts);
        }

        {
            List<URI> hosts = dnsLookupService.resolve(URI.create("srv://_http._tcp")).orTimeout(10, TimeUnit.SECONDS).join();
            System.out.println("srv://_http._tcp "+hosts);
        }

        {
            List<URI> hosts = dnsLookupService.resolve(URI.create("srv://_http._tcp.xorcery.test")).orTimeout(10, TimeUnit.SECONDS).join();
            System.out.println("srv://_http._tcp.xorcery.test "+hosts);
        }


        xorcery2.close();
        logger.info("After server2 shutdown");
        dnsLookupService = xorcery1.getServiceLocator().getService(DnsLookupService.class);
        {
            List<URI> hosts = dnsLookupService.resolve(URI.create("http://server1:80")).orTimeout(10, TimeUnit.SECONDS).join();
            System.out.println(hosts);
        }
        try {
            List<URI> hosts = dnsLookupService.resolve(URI.create("http://server2:80")).orTimeout(10, TimeUnit.SECONDS).join();
            System.out.println(hosts);
        } catch (Throwable t) {
            logger.info("Could not look up server", t);
        }

        {
            List<URI> hosts = dnsLookupService.resolve(URI.create("srv://_servicetest._sub._http._tcp.xorcery.test")).get();
            System.out.println(hosts);
        }

        xorcery1.close();
    }
}
