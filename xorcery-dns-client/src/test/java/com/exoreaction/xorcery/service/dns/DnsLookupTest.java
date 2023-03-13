package com.exoreaction.xorcery.service.dns;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.service.dns.client.DnsLookupService;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
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

    private DnsLookupService dnsLookupService;

    @BeforeEach
    public void setup() {
        dnsLookupService = new DnsLookupService(new Configuration.Builder()
                .add("dns.client.nameservers", instance.arrayNode().add("localhost:8853"))
                .add("dns.client.search", List.of("xorcery.test"))
                .add("dns.client.hosts", instance.objectNode()
                        .put("_certificates._sub._https._tcp.xorcery.test", "https://127.0.0.1:8080/api/path")
                        .put("xorcery1.xorcery.test", "127.0.0.1")
                        .set("certificates.xorcery.test", instance.arrayNode()
                                .add(instance.textNode("127.0.0.1"))
                                .add(instance.textNode("127.0.1.1"))))
                .build(), () -> null);
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
