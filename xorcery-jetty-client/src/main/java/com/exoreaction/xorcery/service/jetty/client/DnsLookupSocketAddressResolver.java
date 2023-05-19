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
package com.exoreaction.xorcery.service.jetty.client;

import com.exoreaction.xorcery.service.dns.client.DnsLookupService;
import com.exoreaction.xorcery.service.dns.client.api.DnsLookup;
import org.eclipse.jetty.util.Promise;
import org.eclipse.jetty.util.SocketAddressResolver;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

public class DnsLookupSocketAddressResolver
        implements SocketAddressResolver {
    private final DnsLookup dnsLookup;

    public DnsLookupSocketAddressResolver(DnsLookupService dnsLookup) {
        this.dnsLookup = dnsLookup;
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
                                List<InetSocketAddress> addressList = new ArrayList<>(list.size());
                                for (URI uri : list) {
                                    addressList.add(new InetSocketAddress(uri.getHost(), uri.getPort()));
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
