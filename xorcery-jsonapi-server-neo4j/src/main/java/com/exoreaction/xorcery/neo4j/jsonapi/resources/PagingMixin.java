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
package com.exoreaction.xorcery.neo4j.jsonapi.resources;

import com.exoreaction.xorcery.jsonapi.Links;
import com.exoreaction.xorcery.jsonapi.server.resources.ResourceContext;
import jakarta.ws.rs.core.UriBuilder;
import org.glassfish.jersey.uri.UriComponent;

import java.util.function.Supplier;

public interface PagingMixin
    extends ResourceContext
{
    // Pagination
    int DEFAULT_LIMIT = 24;

    default Pagination pagination(Links.Builder linksBuilder, Supplier<UriBuilder> uriBuilderSupplier,
                                 String relationshipPath )
    {
        return new Pagination( uriBuilderSupplier,
                UriComponent.decodeQuery( getUriInfo().getRequestUri().getRawQuery(), false, false ), linksBuilder,
                relationshipPath );
    }

    default Pagination pagination( Links.Builder linksBuilder )
    {
        return new Pagination( getUriInfo()::getRequestUriBuilder,
                UriComponent.decodeQuery( getUriInfo().getRequestUri().getRawQuery(), false, false ), linksBuilder, "" );
    }
}
