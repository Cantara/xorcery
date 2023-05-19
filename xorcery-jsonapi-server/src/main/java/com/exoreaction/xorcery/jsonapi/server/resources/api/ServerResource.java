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
package com.exoreaction.xorcery.jsonapi.server.resources.api;

import com.exoreaction.xorcery.configuration.model.Configuration;
import com.exoreaction.xorcery.configuration.model.InstanceConfiguration;
import com.exoreaction.xorcery.jsonapi.model.*;
import com.exoreaction.xorcery.jsonapi.server.resources.JsonApiResource;
import com.exoreaction.xorcery.server.api.ServiceResourceObjects;
import com.exoreaction.xorcery.server.model.ServiceResourceObject;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path( "/" )
public class ServerResource
        extends JsonApiResource
{
    private ServiceResourceObjects serviceResourceObjects;
    private Configuration configuration;

    @Inject
    public ServerResource(ServiceResourceObjects serviceResourceObjects, Configuration configuration)
    {
        this.serviceResourceObjects = serviceResourceObjects;
        this.configuration = configuration;
    }

    @GET
    public ResourceDocument get()
    {
        InstanceConfiguration standardConfiguration = new InstanceConfiguration(configuration.getConfiguration("instance"));

        ResourceObjects.Builder builder = new ResourceObjects.Builder();
        for (ServiceResourceObject serviceResource : serviceResourceObjects.getServiceResources()) {
            builder.resource(serviceResource.resourceObject());
        }
        ResourceDocument serverDocument = new ResourceDocument.Builder()
                .links(new Links.Builder().link(JsonApiRels.self, standardConfiguration.getURI()).build())
                .meta(new Meta.Builder()
                        .meta("timestamp", System.currentTimeMillis())
                        .build())
                .data(builder.build())
                .build();
        return serverDocument.resolve(getBaseUri());
    }
}