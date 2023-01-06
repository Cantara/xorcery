package com.exoreaction.xorcery.service.dns;

import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URI;

public class DnsLookupTest {

    @Test
    public void test() throws MalformedURLException {

        {
            URI uri = URI.create("ws:_events._tcp.hris.test.");
            System.out.println(uri);
            System.out.println(uri.toASCIIString());
            System.out.println(uri.getHost());
        }
        {
            URI uri = URI.create("https:_events._tcp.hris.test.");
            System.out.println(uri);
            System.out.println(uri.toASCIIString());
            System.out.println(uri.getHost());
        }
        {
            URI uri = URI.create("ws://_events._tcp.hris.test./ws/events");
            System.out.println(uri);
            System.out.println(uri.toASCIIString());
            System.out.println(uri.getHost());
        }
    }
}
