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
