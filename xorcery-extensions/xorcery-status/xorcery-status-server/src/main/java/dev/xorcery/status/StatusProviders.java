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
package dev.xorcery.status;

import dev.xorcery.jsonapi.ResourceDocument;
import dev.xorcery.status.spi.StatusProvider;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import org.glassfish.hk2.api.IterableProvider;
import org.jvnet.hk2.annotations.Service;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

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
