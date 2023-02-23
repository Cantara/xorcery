package com.exoreaction.xorcery.service.dns.client;

import com.exoreaction.xorcery.service.dns.client.api.DnsLookup;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class JavaLookup
    implements DnsLookup
{
    @Override
    public CompletableFuture<List<URI>> resolve(URI uri) {
        try {
            InetAddress[] addresses = InetAddress.getAllByName(uri.getHost());
            List<URI> results = new ArrayList<>(addresses.length);
            for (InetAddress address : addresses) {
                // Skip IPv6 for now, it's a headache to use
                if (address instanceof Inet4Address)
                {
                    Inet4Address inet4Address = (Inet4Address) address;
                    URI newUri = new URI(uri.getScheme(), uri.getUserInfo(), inet4Address.getHostAddress(), uri.getPort(), uri.getPath(), uri.getQuery(), uri.getFragment());
                    results.add(newUri);
                }
            }
            return CompletableFuture.completedFuture(results);
        } catch (Throwable e) {
            return CompletableFuture.failedFuture(e);
        }
    }
}
