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
            return lookupSession.lookupAsync(Name.fromString(uri.getHost()), Type.A)
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
                    }).exceptionallyCompose(throwable ->
                    {
                        if (throwable.getCause() instanceof NoSuchDomainException)
                            return CompletableFuture.completedFuture(Collections.emptyList());
                        else
                            return CompletableFuture.failedFuture(throwable);
                    }).toCompletableFuture();
        } catch (TextParseException e) {
            return CompletableFuture.failedFuture(e);
        }
    }
}
