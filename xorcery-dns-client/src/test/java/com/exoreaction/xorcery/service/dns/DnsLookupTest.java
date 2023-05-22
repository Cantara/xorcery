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
package com.exoreaction.xorcery.service.dns;

import com.exoreaction.xorcery.configuration.builder.StandardConfigurationBuilder;
import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.core.Xorcery;
import com.exoreaction.xorcery.service.dns.client.DnsLookupService;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.*;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.xbill.DNS.TextParseException;

import java.io.File;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.fasterxml.jackson.databind.node.JsonNodeFactory.instance;

public class DnsLookupTest {

    private static Xorcery xorcery;
    private static DnsLookupService dnsLookupService;

    private final static String config = """
            dns.client.nameservers:
                - localhost:8853
            dns.client.search:
                - "xorcery.test"
            dns.client.hosts:
                "_certificates._sub._https._tcp.xorcery.test": "https://127.0.0.1:8080/api/path"
                "xorcery1.xorcery.test": "127.0.0.1"
                "certificates.xorcery.test":
                    - "127.0.0.1"
                    - "127.0.1.1"
            """;

    @BeforeAll
    public static void setup() throws Exception {

        xorcery = new Xorcery(new Configuration.Builder().with(new StandardConfigurationBuilder().addTestDefaultsWithYaml(config)).build());

        dnsLookupService = xorcery.getServiceLocator().getService(DnsLookupService.class);
    }

    @AfterAll
    public static void shutdown() {
        xorcery.close();
    }

    @Test
    public void testDnsAFull() throws UnknownHostException, TextParseException, ExecutionException, InterruptedException {

        List<URI> hosts = dnsLookupService.resolve(URI.create("http://xorcery1.xorcery.test:80")).get();
        System.out.println(hosts);
    }

    @Test
    public void testDnsANoDomain() throws UnknownHostException, TextParseException, ExecutionException, InterruptedException {

        List<URI> hosts = dnsLookupService.resolve(URI.create("http://xorcery1:80")).get();
        System.out.println(hosts);
    }

    @Test
    public void testDnsSRVHosts() throws UnknownHostException, TextParseException, ExecutionException, InterruptedException {
        List<URI> hosts = dnsLookupService.resolve(URI.create("srv://_certificates._sub._https._tcp.xorcery.test")).get();
        System.out.println(hosts);
    }

    @Test
    public void testDnsNoDomainSRV() throws UnknownHostException, TextParseException, ExecutionException, InterruptedException {
        List<URI> hosts = dnsLookupService.resolve(URI.create("srv://_certificates._sub._https._tcp")).get();
        System.out.println(hosts);
    }

    @Test
    public void testDnsAList() throws UnknownHostException, TextParseException, ExecutionException, InterruptedException {

        List<URI> hosts = dnsLookupService.resolve(URI.create("http://certificates.xorcery.test:80")).get();
        System.out.println(hosts);
    }

    @Test
    public void testDnsANoDomainList() throws UnknownHostException, TextParseException, ExecutionException, InterruptedException {

        List<URI> hosts = dnsLookupService.resolve(URI.create("http://certificates:80")).get();
        System.out.println(hosts);
    }
}
