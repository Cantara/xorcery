package com.exoreaction.xorcery.service.dns.client;

import com.exoreaction.xorcery.service.dns.client.api.DnsLookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.*;
import org.xbill.DNS.lookup.LookupResult;
import org.xbill.DNS.lookup.LookupSession;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.xbill.DNS.Section.ADDITIONAL;
import static org.xbill.DNS.Section.ANSWER;

public class ALookup
        implements DnsLookup {

    private final LookupSession lookupSession;

    public ALookup(LookupSession lookupSession) {
        this.lookupSession = lookupSession;
    }

    @Override
    public CompletableFuture<List<URI>> resolve(URI uri) {
        try {
            LookupResult lookupResult = lookupSession.lookupAsync(Name.fromString(uri.getHost()), Type.A).toCompletableFuture().join();
            if (lookupResult.getRecords().isEmpty()) {
                return CompletableFuture.completedFuture(Collections.emptyList());
            } else {
                List<URI> servers = new ArrayList<>();
                for (Record record : lookupResult.getRecords()) {
                    ARecord aRecord = (ARecord) record;
                    URI newUri = new URI(uri.getScheme(), uri.getUserInfo(), aRecord.getAddress().getHostAddress(), uri.getPort(), uri.getPath(), uri.getQuery(), uri.getFragment());
                    servers.add(newUri);
                }

                return CompletableFuture.completedFuture(servers);
            }
        } catch (Throwable e) {
            return CompletableFuture.failedFuture(e);
        }
    }
}
