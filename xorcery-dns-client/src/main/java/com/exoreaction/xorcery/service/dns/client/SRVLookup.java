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
import java.util.Objects;
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
                    if (record instanceof TXTRecord) {
                        TXTRecord txtRecord = (TXTRecord) record;
                        for (String txtRecordString : txtRecord.getStrings()) {
                            if (txtRecordString.startsWith("self=")) {
                                path = txtRecordString.substring("self=".length());
                            }
                        }
                    }
                }

                for (Record record : lookupResult.getRecords()) {
                    if (record instanceof SRVRecord) {
                        SRVRecord srvRecord = (SRVRecord) record;
                        String scheme = uri.getScheme();
                        String name = srvRecord.getName().toString(false);
                        if (name.contains("_http.")) {
                            scheme = "http";
                        } else if (name.contains("_https.")) {
                            scheme = "https";
                        }
                        LookupResult serverResult = lookupSession.lookupAsync(srvRecord.getTarget(), Type.A).toCompletableFuture().join();
                        for (Record serverResultRecord : serverResult.getRecords()) {
                            if (serverResultRecord instanceof ARecord) {
                                ARecord aRecord = (ARecord) serverResultRecord;
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

    static final class ServerEntry
                implements Comparable<ServerEntry> {
        private final URI uri;
        private final int priority;
        private final int requests;
        private final int weight;

        ServerEntry(URI uri, int priority, int requests, int weight) {
            this.uri = uri;
            this.priority = priority;
            this.requests = requests;
            this.weight = weight;
        }

        @Override
            public int compareTo(ServerEntry entry) {

                if (entry.priority == priority) {
                    // Same priority, use weight and the nr of requests so far
                    return entry.weight / entry.requests - weight / requests;
                }

                return entry.priority - priority;
            }

        public URI uri() {
            return uri;
        }

        public int priority() {
            return priority;
        }

        public int requests() {
            return requests;
        }

        public int weight() {
            return weight;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (ServerEntry) obj;
            return Objects.equals(this.uri, that.uri) &&
                   this.priority == that.priority &&
                   this.requests == that.requests &&
                   this.weight == that.weight;
        }

        @Override
        public int hashCode() {
            return Objects.hash(uri, priority, requests, weight);
        }

        @Override
        public String toString() {
            return "ServerEntry[" +
                   "uri=" + uri + ", " +
                   "priority=" + priority + ", " +
                   "requests=" + requests + ", " +
                   "weight=" + weight + ']';
        }

        }
}
