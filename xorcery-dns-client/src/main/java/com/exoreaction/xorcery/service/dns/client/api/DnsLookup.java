package com.exoreaction.xorcery.service.dns.client.api;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface DnsLookup {
    CompletableFuture<List<URI>> resolve(URI uri);
}
