package com.exoreaction.reactiveservices.jaxrs.resources;

import com.exoreaction.reactiveservices.cqrs.Command;
import com.exoreaction.reactiveservices.cqrs.Context;
import com.exoreaction.reactiveservices.jsonapi.model.Links;
import jakarta.ws.rs.core.UriBuilder;

import java.util.function.Consumer;

/**
 * Helper methods for JSON:API resource implementations
 *
 */
public interface JsonApiResourceMixin {

    // Links
    default Consumer<Links.Builder> commands(UriBuilder baseUriBuilder, Context context) {
        return links ->
        {
            for ( Command command : context.commands() )
            {
                if (!command.isDelete())
                {
                    String commandName = command.name();
                    links.link( commandName, baseUriBuilder.clone().replaceQueryParam( "rel", commandName ) );
                }
            }
        };
    }


}
