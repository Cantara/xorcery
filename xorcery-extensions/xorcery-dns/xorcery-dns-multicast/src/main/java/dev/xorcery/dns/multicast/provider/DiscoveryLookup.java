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
package dev.xorcery.dns.multicast.provider;

import dev.xorcery.dns.client.api.DnsLookup;
import dev.xorcery.dns.multicast.DnsDiscoveryService;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.glassfish.hk2.api.Rank;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

import javax.jmdns.ServiceEvent;
import java.net.InetAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@Service
@ContractsProvided(DnsLookup.class)
@Rank(2)
public class DiscoveryLookup
        implements DnsLookup {
    private final Supplier<DnsDiscoveryService> discoveryService;

    @Inject
    public DiscoveryLookup(Provider<DnsDiscoveryService> discoveryService) {
        this.discoveryService = discoveryService::get;
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
