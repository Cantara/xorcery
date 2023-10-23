package com.exoreaction.xorcery.status;

import com.exoreaction.xorcery.configuration.Configuration;
import com.exoreaction.xorcery.jsonapi.ResourceDocument;
import com.exoreaction.xorcery.status.spi.StatusProvider;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import org.glassfish.hk2.api.IterableProvider;
import org.jvnet.hk2.annotations.Service;

import java.util.*;

@Service(name = "status")
public class StatusProviders {

    private final Map<String, StatusProvider> statusProviderMap;

    @Inject
    public StatusProviders(
            IterableProvider<StatusProvider> statusProvider
    ) {
        statusProviderMap = new TreeMap<>(String::compareTo);
        for (StatusProvider provider : statusProvider) {
            statusProviderMap.put(provider.getId(), provider);
        }
    }

    public Iterable<String> getStatusProviderNames() {
        return statusProviderMap.keySet();
    }

    public ResourceDocument getResourceDocument(String name, String include)
            throws NotFoundException {
        return Optional.ofNullable(statusProviderMap.get(name))
                .map(provider -> provider.getResourceDocument(include))
                .orElseThrow(NotFoundException::new);
    }

}
