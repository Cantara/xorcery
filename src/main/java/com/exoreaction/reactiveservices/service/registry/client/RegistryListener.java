package com.exoreaction.reactiveservices.service.registry.client;

import com.exoreaction.reactiveservices.jsonapi.ResourceDocument;
import com.exoreaction.reactiveservices.jsonapi.ResourceObject;

/**
 * @author rickardoberg
 * @since 13/04/2022
 */

public interface RegistryListener
{
    default void snapshot( ResourceDocument registry )
    {
        registry.getResources().ifPresent( ro ->
        {
            ro.getResources().forEach( this::added );
        });
    }

    default void added( ResourceDocument server)
    {
        server.getResources().ifPresent( ro ->
        {
            ro.getResources().forEach( this::added );
        });
    }

    void added( ResourceObject service);
    void updated( ResourceObject service);
    void removed( ResourceObject service);
}
