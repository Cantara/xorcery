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
package dev.xorcery.status.api;

import dev.xorcery.jsonapi.ResourceDocument;
import dev.xorcery.status.StatusProviders;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;

import java.util.Optional;

@Path("api/status/{name}")
public class StatusProviderResource {

    private final StatusProviders statusProviders;

    @Inject
    public StatusProviderResource(StatusProviders statusProviders) {
        this.statusProviders = statusProviders;
    }

    @GET
    public ResourceDocument get(@PathParam("name") String name, @QueryParam("include") String include)
    {
        return statusProviders.getResourceDocument(name, Optional.ofNullable(include).orElse(""));
    }
}
