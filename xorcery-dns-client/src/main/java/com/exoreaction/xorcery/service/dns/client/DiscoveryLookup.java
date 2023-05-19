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
import com.exoreaction.xorcery.service.dns.client.discovery.DnsDiscoveryService;

import javax.jmdns.ServiceEvent;
import java.net.InetAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class DiscoveryLookup
        implements DnsLookup {
    private Supplier<DnsDiscoveryService> discoveryService;

    public DiscoveryLookup(Supplier<DnsDiscoveryService> discoveryService) {
        this.discoveryService = discoveryService;
    }

    @Override
    public CompletableFuture<List<URI>> resolve(URI uri) {

        if (uri.getScheme().equals("srv")) {
            DnsDiscoveryService service = discoveryService.get();
            if (service != null) {
                List<ServiceEvent> services = service.getServices().get(uri.getAuthority());
                if (services != null)
                {
                    List<URI> uris = new ArrayList<>(services.size());
                    for (ServiceEvent serviceEvent : services) {
                        for (InetAddress inetAddress : serviceEvent.getInfo().getInetAddresses()) {
                            String scheme = serviceEvent.getInfo().getApplication();
                            for (String url : serviceEvent.getInfo().getURLs()) {
                                uris.add(URI.create(url));
                            }
                        }
                    }
                    return CompletableFuture.completedFuture(uris);
                }
            }
        }

        return CompletableFuture.completedFuture(Collections.emptyList());
    }
}
