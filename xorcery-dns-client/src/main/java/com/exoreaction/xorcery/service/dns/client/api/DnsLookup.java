package com.exoreaction.xorcery.service.dns.client.api;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Lookup API to convert a URI with names (hostnames, SRV names, etc.) into a list of URIs with IPs, schemes and other information.
 */
public interface DnsLookup {
    CompletableFuture<List<URI>> resolve(URI uri);
}
