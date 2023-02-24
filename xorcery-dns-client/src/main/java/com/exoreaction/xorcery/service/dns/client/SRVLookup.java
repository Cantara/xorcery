package com.exoreaction.xorcery.service.dns.client;

import com.exoreaction.xorcery.service.dns.client.api.DnsLookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.*;
import org.xbill.DNS.lookup.LookupResult;
import org.xbill.DNS.lookup.LookupSession;
import org.xbill.DNS.lookup.NoSuchDomainException;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class SRVLookup
        implements DnsLookup {

    private final LookupSession lookupSession;
    private final Map<URI, AtomicInteger> serverRequests = new ConcurrentHashMap<>();

    public SRVLookup(LookupSession lookupSession) {
        this.lookupSession = lookupSession;
    }

    @Override
    public CompletableFuture<List<URI>> resolve(URI uri) {

        String authority = uri.getAuthority();
        if (!authority.startsWith("_")) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        try {
            LookupResult lookupResult = lookupSession.lookupAsync(Name.fromString(authority), Type.SRV).toCompletableFuture().join();

            if (lookupResult.getRecords().isEmpty()) {
                return CompletableFuture.completedFuture(Collections.emptyList());
            } else {
                List<URI> servers = new ArrayList<>();

                // Get TXT record
                LookupResult txtResult = lookupSession.lookupAsync(Name.fromString(authority), Type.TXT).toCompletableFuture().join();
                String path = uri.getPath();
                for (Record record : txtResult.getRecords()) {
                    if (record instanceof TXTRecord txtRecord) {
                        for (String txtRecordString : txtRecord.getStrings()) {
                            if (txtRecordString.startsWith("self=")) {
                                path = txtRecordString.substring("self=".length());
                            }
                        }
                    }
                }

                for (Record record : lookupResult.getRecords()) {
                    if (record instanceof SRVRecord srvRecord) {
                        String scheme = uri.getScheme();
                        String name = srvRecord.getName().toString(false);
                        if (name.contains("_http.")) {
                            scheme = "http";
                        } else if (name.contains("_https.")) {
                            scheme = "https";
                        }
                        LookupResult serverResult = lookupSession.lookupAsync(srvRecord.getTarget(), Type.A).toCompletableFuture().join();
                        for (Record serverResultRecord : serverResult.getRecords()) {
                            if (serverResultRecord instanceof ARecord aRecord) {
                                servers.add(new URI(scheme, uri.getUserInfo(), aRecord.getAddress().getHostAddress(), srvRecord.getPort(), path, uri.getQuery(), uri.getFragment()));
                            }
                        }
                    }
                }

/*
                if (!servers.isEmpty()) {
                    // Sort based on priority and weight
                    Collections.sort(servers);
                    System.out.println("Sorted:" + servers);
                    serverRequests.get(servers.get(0)).incrementAndGet();
                }
*/

                return CompletableFuture.completedFuture(servers);
            }
        } catch (Throwable e) {
            if (e.getCause() instanceof NoSuchDomainException)
            {
                return CompletableFuture.completedFuture(Collections.emptyList());
            } else
            {
                return CompletableFuture.failedFuture(e);
            }
        }
    }

    record ServerEntry(URI uri, int priority, int requests, int weight)
            implements Comparable<ServerEntry> {
        @Override
        public int compareTo(ServerEntry entry) {

            if (entry.priority == priority) {
                // Same priority, use weight and the nr of requests so far
                return entry.weight / entry.requests - weight / requests;
            }

            return entry.priority - priority;
        }
    }
}
