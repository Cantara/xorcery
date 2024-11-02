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
package dev.xorcery.jetty.client.providers;

import dev.xorcery.dns.client.api.DnsLookup;
import dev.xorcery.dns.client.providers.DnsLookupService;
import org.eclipse.jetty.util.Promise;
import org.eclipse.jetty.util.SocketAddressResolver;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Use the Xorcery DNS lookup service to resolve names.
 * Apart from normal DNS lookups this would also include manually configured nameservers, multicast discoveries, and mappings in the Configuration.
 */
public class DnsLookupSocketAddressResolver
        implements SocketAddressResolver {
    private final DnsLookup dnsLookup;

    private final Random random;

    public DnsLookupSocketAddressResolver(DnsLookupService dnsLookup) {
        this.dnsLookup = dnsLookup;
        random = new Random();
    }

    @Override
    public void resolve(String host, int port, Promise<List<InetSocketAddress>> promise) {

        try {
            dnsLookup.resolve(new URI("http", null, host, port, null, null, null))
                    .whenComplete((list, t) ->
                    {
                        if (t != null) {
                            promise.failed(t);
                        } else {
                            try {
                                // Randomly shift the list so that different clients see different orders of IPs
                                int size = list.size();
                                List<InetSocketAddress> addressList = new ArrayList<>(size);
                                int shift = random.nextInt(size);
                                for (int i = 0; i < size; i++) {
                                    URI uri = list.get((i+shift)%size);
                                    InetAddress inetAddress = InetAddress.getByAddress(host, InetAddress.getByName(uri.getHost()).getAddress());
                                    addressList.add(new InetSocketAddress(inetAddress, uri.getPort()));
                                }
                                promise.succeeded(addressList);
                            } catch (Exception e) {
                                promise.failed(e);
                            }
                        }
                    });
        } catch (URISyntaxException e) {
            promise.failed(e);
        }
    }
}
