package com.exoreaction.reactiveservices.jsonapi.resources;

import com.exoreaction.reactiveservices.jsonapi.model.Links;
import com.exoreaction.reactiveservices.service.neo4j.client.GraphQuery;
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
