package com.exoreaction.xorcery.service.dns.client;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.service.dns.client.api.DnsLookup;
import com.exoreaction.xorcery.service.dns.client.discovery.DnsDiscoveryService;
import com.exoreaction.xorcery.util.Sockets;
import com.fasterxml.jackson.databind.JsonNode;
import org.xbill.DNS.*;
import org.xbill.DNS.hosts.HostsFileParser;
import org.xbill.DNS.lookup.LookupSession;

import java.io.UncheckedIOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static com.exoreaction.xorcery.util.Exceptions.unwrap;

public class DnsLookupService
        implements DnsLookup {

    private final List<DnsLookup> lookups = new ArrayList<>();

    public DnsLookupService(Iterable<DnsLookup> dnsLookups) {
        dnsLookups.forEach(lookups::add);
    }

    public DnsLookupService(Configuration configuration, Supplier<DnsDiscoveryService> dnsDiscoveryServiceSupplier) {
        LookupSession lookupSession = new DnsLookupSessionFactory(configuration).provide();
        lookups.add(new HostsConfigurationLookup(configuration));
        lookups.add(new SRVLookup(lookupSession));
        lookups.add(new ALookup(lookupSession));
        Optional.ofNullable(dnsDiscoveryServiceSupplier.get())
                .ifPresent(srv -> lookups.add(new DiscoveryLookup(() -> srv)));
    }

    @Override
    public CompletableFuture<List<URI>> resolve(URI uri) {

        try {
            if (validIP(uri.getHost())) {
                return CompletableFuture.completedFuture(List.of(uri));
            }

            for (DnsLookup lookup : lookups) {
                CompletableFuture<List<URI>> result = lookup.resolve(uri);
                if (!result.get().isEmpty())
                    return result;
            }
            return CompletableFuture.completedFuture(List.of(uri));
        } catch (Throwable e) {
            return CompletableFuture.failedFuture(unwrap(e));
        }
    }

    public static boolean validIP(String ip) {
        if (ip == null || ip.length() < 7 || ip.length() > 15) return false;

        try {
            int x = 0;
            int y = ip.indexOf('.');

            if (y == -1 || ip.charAt(x) == '-' || Integer.parseInt(ip.substring(x, y)) > 255) return false;

            x = ip.indexOf('.', ++y);
            if (x == -1 || ip.charAt(y) == '-' || Integer.parseInt(ip.substring(y, x)) > 255) return false;

            y = ip.indexOf('.', ++x);
            return !(y == -1 ||
                    ip.charAt(x) == '-' ||
                    Integer.parseInt(ip.substring(x, y)) > 255 ||
                    ip.charAt(++y) == '-' ||
                    Integer.parseInt(ip.substring(y, ip.length())) > 255 ||
                    ip.charAt(ip.length() - 1) == '.');

        } catch (NumberFormatException e) {
            return false;
        }
    }
}
