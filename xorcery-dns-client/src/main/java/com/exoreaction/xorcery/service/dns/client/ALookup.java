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
