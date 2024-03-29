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
package com.exoreaction.xorcery.test.resources;

import com.exoreaction.xorcery.jaxrs.server.resources.BaseResource;
import com.exoreaction.xorcery.jsonapi.Attributes;
import com.exoreaction.xorcery.jsonapi.Links;
import com.exoreaction.xorcery.jsonapi.ResourceDocument;
import com.exoreaction.xorcery.jsonapi.ResourceObject;
import com.exoreaction.xorcery.jsonapi.server.resources.JsonApiResource;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("api/service")
public class ServiceResource
        extends BaseResource
        implements JsonApiResource {

    @GET
    public ResourceDocument get() {

        ResourceDocument resourceDocument = new ResourceDocument.Builder()
                .links(new Links.Builder()
                        .link("self", getUriInfo().getRequestUri())
                        .build())
                .data(new ResourceObject.Builder("service").attributes(new Attributes.Builder().with(b ->
                {
                    b.attribute("foo", "bar")
                            .attribute("host", getHttpServletRequest().getHeader("host"));
                }).build()).build()).build();

        return resourceDocument;
    }
}
