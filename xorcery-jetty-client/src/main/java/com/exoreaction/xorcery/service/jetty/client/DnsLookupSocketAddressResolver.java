package com.exoreaction.xorcery.service.jetty.client;

import com.exoreaction.xorcery.service.dns.client.DnsLookupService;
import com.exoreaction.xorcery.service.dns.client.api.DnsLookup;
import org.eclipse.jetty.util.Promise;
import org.eclipse.jetty.util.SocketAddressResolver;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class DnsLookupSocketAddressResolver
        implements SocketAddressResolver {
    private final DnsLookup dnsLookup;

    public DnsLookupSocketAddressResolver(DnsLookupService dnsLookup) {
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
