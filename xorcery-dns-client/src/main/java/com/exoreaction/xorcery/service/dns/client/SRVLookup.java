package com.exoreaction.xorcery.service.dns.client;

import com.exoreaction.xorcery.service.dns.client.api.DnsLookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.*;
import org.xbill.DNS.lookup.LookupResult;
import org.xbill.DNS.lookup.LookupSession;

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

        if (!uri.getAuthority().startsWith("_"))
        {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        try {
            LookupResult lookupResult = lookupSession.lookupAsync(Name.fromString(uri.getAuthority()), Type.SRV).toCompletableFuture().join();

            if (lookupResult.getRecords().isEmpty()) {
                return CompletableFuture.completedFuture(Collections.emptyList());
            } else {
                List<URI> servers = new ArrayList<>();

                // Get TXT record
                LookupResult txtResult = lookupSession.lookupAsync(Name.fromString(uri.getAuthority()), Type.TXT).toCompletableFuture().join();
                String scheme = uri.getScheme();
                String path = uri.getPath();
                for (Record record : txtResult.getRecords()) {
                    if (record instanceof TXTRecord txtRecord)
                    {
                        for (String txtRecordString : txtRecord.getStrings()) {
                            if (txtRecordString.startsWith("api_scheme="))
                            {
                                scheme = txtRecordString.substring("api_scheme=".length());
                            }
                            else if (txtRecordString.startsWith("api_path="))
                            {
                                path = txtRecordString.substring("api_path=".length());
                            }
                        }
                    }
                }

                for (Record record : lookupResult.getRecords()) {
                    if (record instanceof SRVRecord srvRecord)
                    {
                        LookupResult serverResult = lookupSession.lookupAsync(srvRecord.getTarget(), Type.A).toCompletableFuture().join();
                        for (Record serverResultRecord : serverResult.getRecords()) {
                            if (serverResultRecord instanceof ARecord aRecord)
                            {
                                servers.add(new URI(scheme, uri.getUserInfo(), ((ARecord) serverResultRecord).getAddress().getHostAddress(), srvRecord.getPort(), path, uri.getQuery(), uri.getFragment()));
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
//                return CompletableFuture.completedFuture(servers.stream().map(ServerEntry::address).collect(Collectors.toList()));
            }
        } catch (Throwable e) {
            return CompletableFuture.failedFuture(e);
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
