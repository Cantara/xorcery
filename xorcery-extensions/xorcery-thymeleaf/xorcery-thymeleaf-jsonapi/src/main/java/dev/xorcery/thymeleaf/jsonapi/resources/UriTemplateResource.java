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
package dev.xorcery.thymeleaf.jsonapi.resources;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.*;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Path("api/thymeleaf/uritemplate")
public class UriTemplateResource {
    @GET
    public Response get(@Context UriInfo uriInfo) {
        MultivaluedMap<String, String> queryParameters = uriInfo.getQueryParameters();
        Map<String, Object> cleanedParameters = new HashMap<>();
        for (String name : queryParameters.keySet()) {
            cleanedParameters.put(name, Optional.ofNullable(queryParameters.getFirst(name)).orElse(""));
        }
        URI redirectUri = UriBuilder.fromUri(queryParameters.getFirst("uritemplate"))
                .resolveTemplates(cleanedParameters).build();
        return Response.temporaryRedirect(redirectUri).build();
    }
}
