/*
 * Copyright © 2022 eXOReaction AS (rickard@exoreaction.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.exoreaction.xorcery.dns.client.providers;

import com.exoreaction.xorcery.dns.client.api.DnsLookup;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xbill.DNS.Record;
import org.xbill.DNS.*;
import org.xbill.DNS.lookup.LookupResult;
import org.xbill.DNS.lookup.LookupSession;
import org.xbill.DNS.lookup.NoSuchDomainException;
import org.xbill.DNS.lookup.NoSuchRRSetException;

import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class SRVLookup
        implements DnsLookup {

    private final LookupSession lookupSession;
    private final Map<URI, List<URI>> currentUriMappings = new ConcurrentHashMap<>(); // Track current mapping from srv URI to list of URIs
    private final Map<URI, Map<String, AtomicLong>> uriServerRequests = new ConcurrentHashMap<>(); // Track current request counts for URIs of a particular srv URI
    private final Logger logger = LogManager.getLogger(getClass());

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
            Name srvName = Name.fromString(authority);

            // Convert scheme from name, e.g. _foobar._sub._https._tcp.somedomain.com -> https
            String scheme = uri.getScheme();
            for (int i = srvName.labels() - 1; i > 0; i--) {
                if (srvName.getLabelString(i).equals("_tcp")) {
                    scheme = srvName.getLabelString(i - 1).substring(1);
                    break;
                }
            }
            LookupResult srvLookupResult = lookupSession.lookupAsync(srvName, Type.SRV).toCompletableFuture().join();

            if (srvLookupResult.getRecords().isEmpty()) {
                return CompletableFuture.failedFuture(new NoSuchRRSetException(srvName, Type.SRV));
            } else {
                List<ServerEntry> servers = new ArrayList<>();
                List<URI> serverURIs = new ArrayList<>();

                // Get TXT record and extract path
                LookupResult txtResult = lookupSession.lookupAsync(srvName, Type.TXT).toCompletableFuture().join();
                String path = uri.getPath();
                for (Record record : txtResult.getRecords()) {
                    if (record instanceof TXTRecord txtRecord) {
                        for (String txtRecordString : txtRecord.getStrings()) {
                            if (txtRecordString.startsWith("self=")) {
                                path = txtRecordString.substring("self=".length());
                                break;
                            }
                        }
                    }
                }

                Map<String, AtomicLong> serverRequests = uriServerRequests.computeIfAbsent(uri, u -> new ConcurrentHashMap<>());
                for (Record record : srvLookupResult.getRecords()) {
                    if (record instanceof SRVRecord srvRecord) {

                        URI serverUri = new URI(scheme, uri.getUserInfo(), srvRecord.getTarget().toString(true), srvRecord.getPort(), path, uri.getQuery(), uri.getFragment());
                        serverURIs.add(serverUri);
                        servers.add(new ServerEntry(serverUri,
                                srvRecord.getPriority(), serverRequests.computeIfAbsent(serverUri.getAuthority(), u -> new AtomicLong(1)).get(), srvRecord.getWeight()));
                    }
                }

                // Sort based on priority and weight
                if (!servers.isEmpty()) {
                    Collections.sort(servers);
                    if (logger.isTraceEnabled()) {
                        logger.trace("Sorted {}: {}", uri, servers);
                    }
                    serverRequests.get(servers.get(0).uri().getAuthority()).incrementAndGet();
                }

                // Check if URI list has changed and we should reset the counts
                List<URI> currentMappings = this.currentUriMappings.get(uri);
                if (currentMappings == null) {
                    currentUriMappings.put(uri, serverURIs);
                } else if (!currentMappings.equals(serverURIs)) {
                    // Mapping from SRV to list of URIs has changed, reset the request counts
                    serverRequests.clear();
                    currentUriMappings.put(uri, serverURIs);
                    if (logger.isTraceEnabled()) {
                        logger.trace("Reset request counts for {}", uri);
                    }
                }

                return CompletableFuture.completedFuture(servers.stream().map(ServerEntry::uri).collect(Collectors.toList()));
            }
        } catch (Throwable e) {
            if (e.getCause() instanceof NoSuchDomainException) {
                return CompletableFuture.completedFuture(Collections.emptyList());
            } else {
                return CompletableFuture.failedFuture(e);
            }
        }
    }

    public record ServerEntry(URI uri, int priority, long requests, long weight)
            implements Comparable<ServerEntry> {
        @Override
        public int compareTo(ServerEntry entry) {

            if (entry.priority == priority) {

                long totalWeight = entry.weight + weight;

                // Same priority, use weight and the nr of requests so far
                return Long.compare(100L * requests / (totalWeight - entry.weight), 100L * entry.requests / (totalWeight - weight));
            }

            // Lowest priority should come first
            return Long.compare(priority, entry.priority);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ServerEntry that = (ServerEntry) o;
            return Objects.equals(uri, that.uri);
        }

        @Override
        public int hashCode() {
            return Objects.hash(uri);
        }
    }
}
