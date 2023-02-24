package com.exoreaction.xorcery.service.dns.client;

import com.exoreaction.xorcery.service.dns.client.api.DnsLookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.*;
import org.xbill.DNS.lookup.LookupSession;
import org.xbill.DNS.lookup.NoSuchDomainException;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class ALookup
        implements DnsLookup {

    private final LookupSession lookupSession;

    public ALookup(LookupSession lookupSession) {
        this.lookupSession = lookupSession;
    }

    @Override
    public CompletableFuture<List<URI>> resolve(URI uri) {
        try {
            String host = uri.getHost();
            if (host == null)
            {
                return CompletableFuture.completedFuture(Collections.emptyList());
            }
            return lookupSession.lookupAsync(Name.fromString(host), Type.A)
                    .<List<URI>>thenApply(lookupResult ->
                    {
                        if (lookupResult.getRecords().isEmpty()) {
                            return Collections.emptyList();
                        } else {
                            try {
                                List<URI> servers = new ArrayList<>();
                                for (Record record : lookupResult.getRecords()) {
                                    ARecord aRecord = (ARecord) record;
                                    URI newUri = new URI(uri.getScheme(), uri.getUserInfo(), aRecord.getAddress().getHostAddress(), uri.getPort(), uri.getPath(), uri.getQuery(), uri.getFragment());
                                    servers.add(newUri);
                                }
                                return servers;
                            } catch (Throwable e) {
                                throw new CompletionException(e);
                            }
                        }
                    }).handle((uris, throwable) -> {
                        if (throwable != null) {
                            if (throwable.getCause() instanceof NoSuchDomainException)
                                return Collections.<URI>emptyList();
                            else
                                throw new CompletionException(throwable);
                        } else {
                            return uris;
                        }
                    })
                    .toCompletableFuture();
        } catch (TextParseException e) {
            return CompletableFuture.failedFuture(e);
        }
    }
}
