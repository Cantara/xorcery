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
package dev.xorcery.admin.api;

import dev.xorcery.health.registry.DefaultHealthCheckService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.*;
import jakarta.ws.rs.ext.RuntimeDelegate;

@Path("/health")
public class VisualeHealthResource {

    private static final CacheControl cacheControl = RuntimeDelegate.getInstance()
            .createHeaderDelegate(CacheControl.class)
            .fromString("must-revalidate,no-cache,no-store");

    private final DefaultHealthCheckService healthCheckService;

    @Inject
    public VisualeHealthResource(DefaultHealthCheckService defaultHealthCheckService) {
        this.healthCheckService = defaultHealthCheckService;
    }

    @GET
    public Response get(@QueryParam("compact") String compact) {
        boolean prettyPrint = !Boolean.parseBoolean(compact);
        StreamingOutput stream = out -> healthCheckService.writeHealthState(out, prettyPrint);
        return Response.ok(stream)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .cacheControl(cacheControl)
                .build();
    }
}
