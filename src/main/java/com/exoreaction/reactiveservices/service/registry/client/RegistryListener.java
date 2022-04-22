package com.exoreaction.reactiveservices.service.registry.client;

import com.exoreaction.reactiveservices.jsonapi.ResourceDocument;
import com.exoreaction.reactiveservices.jsonapi.ResourceObject;
import com.exoreaction.reactiveservices.jsonapi.ResourceObjects;

/**
 * @author rickardoberg
 * @since 13/04/2022
 */

public interface RegistryListener
{
    default void registry(ResourceDocument registry)
    {
    }

    default void servers( ResourceDocument servers )
    {
        servers.getResources().ifPresent(resources ->
        {
            resources.getResources().forEach( ro ->
            {
                switch (ro.getType())
                {
                    case "server": addedServer(ro);
                    case "service": addedService(ro);
                }
            } );
        });
    }

    default void addedServer( ResourceObject server)
    {
    }

    default void removedServer( ResourceObject server)
    {
    }

    default void addedService( ResourceObject service)
    {

    }
    default void updatedService( ResourceObject service)
    {

    }
    default void removedService( ResourceObject service)
    {

    }
}
