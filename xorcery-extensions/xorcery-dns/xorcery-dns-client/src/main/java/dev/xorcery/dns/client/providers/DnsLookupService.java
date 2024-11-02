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
package dev.xorcery.dns.client.providers;

import dev.xorcery.concurrent.CompletableFutures;
import dev.xorcery.dns.client.api.DnsLookup;

import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

public class DnsLookupService
        implements DnsLookup {

    private final List<DnsLookup> lookups = new ArrayList<>();

    public DnsLookupService(Iterable<DnsLookup> dnsLookups) {
        dnsLookups.forEach(lookups::add);
        if (lookups.isEmpty())
            throw new IllegalStateException("No DNS lookup handlers found");
    }

    @Override
    public CompletableFuture<List<URI>> resolve(URI uri) {

        if (validIP(uri.getHost())) {
            return CompletableFuture.completedFuture(List.of(uri));
        }

        CompletableFuture<List<URI>> result = new CompletableFuture<>();
        Iterator<DnsLookup> dnsLookupIterator = lookups.iterator();

        if (dnsLookupIterator.hasNext()) {
            dnsLookupIterator.next().resolve(uri).thenCompose(handleResult(uri, dnsLookupIterator))
                    .whenComplete(CompletableFutures.transfer(result));
        }

        return result;
    }

    private Function<List<URI>, CompletionStage<List<URI>>> handleResult(URI uri, Iterator<DnsLookup> dnsLookupIterator) {
        return r ->
        {
            if (r.isEmpty()) {
                if (dnsLookupIterator.hasNext())
                    return dnsLookupIterator.next().resolve(uri).thenCompose(handleResult(uri, dnsLookupIterator));
                else
                    return CompletableFuture.failedStage(new UnknownHostException(uri.getHost()));
            } else {
                return CompletableFuture.completedStage(r);
            }
        };
    }

    public static boolean validIP(String ip) {
        if (ip == null || ip.length() < 7 || ip.length() > 15) return false;

        try {
            int x = 0;
            int y = ip.indexOf('.');

            if (y == -1 || ip.charAt(x) == '-' || Integer.parseInt(ip.substring(x, y)) > 255) return false;

            x = ip.indexOf('.', ++y);
            if (x == -1 || ip.charAt(y) == '-' || Integer.parseInt(ip.substring(y, x)) > 255) return false;

            y = ip.indexOf('.', ++x);
            return !(y == -1 ||
                    ip.charAt(x) == '-' ||
                    Integer.parseInt(ip.substring(x, y)) > 255 ||
                    ip.charAt(++y) == '-' ||
                    Integer.parseInt(ip.substring(y, ip.length())) > 255 ||
                    ip.charAt(ip.length() - 1) == '.');

        } catch (NumberFormatException e) {
            return false;
        }
    }
}
