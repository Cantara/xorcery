package com.exoreaction.xorcery.service.dns;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.service.dns.client.DnsLookupService;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.xbill.DNS.*;
import org.xbill.DNS.Record;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.xbill.DNS.Record.newRecord;
import static org.xbill.DNS.Section.ANSWER;

@Disabled
public class DnsLookupTest {

    private DnsLookupService dnsLookupService;

    @BeforeEach
    public void setup()
    {
        dnsLookupService = new DnsLookupService(new Configuration.Builder()
                .add("dns.search", List.of("xorcery.test."))
                .build());
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
    public void testDnsSRV() throws UnknownHostException, TextParseException, ExecutionException, InterruptedException {
        List<URI> hosts = dnsLookupService.resolve(URI.create("srv://_analytics._tcp.xorcery.test")).get();
        System.out.println(hosts);
    }

    @Test
    public void testDnsAList() throws UnknownHostException, TextParseException, ExecutionException, InterruptedException {

        List<URI> hosts = dnsLookupService.resolve(URI.create("http://analytics.xorcery.test")).get();
        System.out.println(hosts);
    }
    @Test
    public void testDnsANoDomainList() throws UnknownHostException, TextParseException, ExecutionException, InterruptedException {

        List<URI> hosts = dnsLookupService.resolve(URI.create("http://analytics")).get();
        System.out.println(hosts);
    }
}
