package com.exoreaction.xorcery.service.jetty.client.dns;

import com.exoreaction.xorcery.service.dns.client.api.DnsLookup;
import org.eclipse.jetty.util.Promise;
import org.eclipse.jetty.util.SocketAddressResolver;
import org.xbill.DNS.Record;
import org.xbill.DNS.*;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.xbill.DNS.Section.ADDITIONAL;
import static org.xbill.DNS.Section.ANSWER;

public class DnsLookupSocketAddressResolver
        implements SocketAddressResolver {
    private final DnsLookup dnsLookup;

    public DnsLookupSocketAddressResolver(DnsLookup dnsLookup) {
        this.dnsLookup = dnsLookup;
    }

    @Override
    public void resolve(String host, int port, Promise<List<InetSocketAddress>> promise) {

        try {
            dnsLookup.resolve(new URI("http", null, host, port, null, null, null))
                    .whenComplete((list, t) ->
                    {
                        if (t != null) {
                            promise.failed(t);
                        } else {
                            try {
                                List<InetSocketAddress> addressList = new ArrayList<>(list.size());
                                for (URI uri : list) {
                                    addressList.add(new InetSocketAddress(uri.getHost(), uri.getPort()));
                                }
                                promise.succeeded(addressList);
                            } catch (Exception e) {
                                promise.failed(e);
                            }
                        }
                    });
        } catch (URISyntaxException e) {
            promise.failed(e);
        }
    }
}
