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
package com.exoreaction.xorcery.jsonapi.service;

import com.exoreaction.xorcery.jsonapi.Link;
import com.exoreaction.xorcery.jsonapi.ResourceDocument;

import java.util.Optional;
import java.util.stream.Stream;

public record ServerResourceDocument(ResourceDocument resourceDocument) {

    public Link getSelf()
    {
        return resourceDocument.getLinks().getSelf().orElseThrow();
    }

    public Stream<ServiceResourceObject> getServices() {
        return resourceDocument.getResources().orElseThrow()
                .stream()
                .map(ServiceResourceObject::new);
    }

    public Optional<ServiceResourceObject> getServiceByIdentifier(ServiceIdentifier serviceIdentifier) {
        return resourceDocument.getResources()
                .flatMap(r -> r.stream()
                        .filter(ro -> ro.getResourceObjectIdentifier().equals(serviceIdentifier.resourceObjectIdentifier()))
                        .findFirst())
                .map(ServiceResourceObject::new);
    }

    public Optional<ServiceResourceObject> getServiceByType(String type) {
        return resourceDocument.getResources()
                .flatMap(ro -> ro.stream()
                        .filter(r -> r.getType().equals(type))
                        .findFirst())
                .map(ServiceResourceObject::new);
    }
}
