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

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class JavaLookup
    implements DnsLookup
{
    @Override
    public CompletableFuture<List<URI>> resolve(URI uri) {
        try {
            InetAddress[] addresses = InetAddress.getAllByName(uri.getHost());
            List<URI> results = new ArrayList<>(addresses.length);
            for (InetAddress address : addresses) {
                // Skip IPv6 for now, it's a headache to use
                if (address instanceof Inet4Address inet4Address)
                {
                    URI newUri = new URI(uri.getScheme(), uri.getUserInfo(), inet4Address.getHostAddress(), uri.getPort(), uri.getPath(), uri.getQuery(), uri.getFragment());
                    results.add(newUri);
                }
            }
            return CompletableFuture.completedFuture(results);
        } catch (Throwable e) {
            return CompletableFuture.failedFuture(e);
        }
    }
}
