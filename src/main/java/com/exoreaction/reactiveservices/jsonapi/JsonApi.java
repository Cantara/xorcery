package com.exoreaction.reactiveservices.jsonapi;

import com.exoreaction.reactiveservices.json.AbstractJsonElement;

import jakarta.json.JsonStructure;

/**
 * @author rickardoberg
 * @since 27/11/2018
 */

public class JsonApi
    extends AbstractJsonElement
{
    public JsonApi( JsonStructure json )
    {
        super( json );
    }
}
