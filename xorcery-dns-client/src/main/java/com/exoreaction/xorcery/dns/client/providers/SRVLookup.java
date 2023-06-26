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

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class SRVLookup
        implements DnsLookup {

    private final LookupSession lookupSession;
    private final Map<String, AtomicLong> serverRequests = new ConcurrentHashMap<>();

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
                List<ServerEntry> servers = new ArrayList<>();

                // Get TXT record
                LookupResult txtResult = lookupSession.lookupAsync(Name.fromString(authority), Type.TXT).toCompletableFuture().join();
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
                                URI serverUri = new URI(scheme, uri.getUserInfo(), aRecord.getAddress().getHostAddress(), srvRecord.getPort(), path, uri.getQuery(), uri.getFragment());
                                servers.add(new ServerEntry(serverUri,
                                        srvRecord.getPriority(), serverRequests.computeIfAbsent(serverUri.getAuthority(), u -> new AtomicLong(1)).get(), srvRecord.getWeight()));
                            }
                        }
                    }
                }

                if (!servers.isEmpty()) {
                    // Sort based on priority and weight
                    Collections.sort(servers);
                    Logger logger = LogManager.getLogger();
                    if (logger.isTraceEnabled()) {
                        logger.trace("Sorted {}: {}", uri, servers);
                    }
                    serverRequests.get(servers.get(0).uri().getAuthority()).incrementAndGet();
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
    }
}
