package com.exoreaction.reactiveservices.jsonapi;

import com.exoreaction.reactiveservices.json.AbstractJsonElement;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;

import java.util.List;

/**
 * @author rickardoberg
 * @since 28/11/2018
 */

public class ResourceObjectIdentifiers
    extends AbstractJsonElement
{
    public static class Builder
    {
        JsonArrayBuilder builder = Json.createArrayBuilder();

        public Builder resource( ResourceObjectIdentifier resourceObjectIdentifier )
        {
            builder.add( resourceObjectIdentifier.json() );
            return this;
        }

        public Builder resource( ResourceObject resourceObject )
        {
            builder.add( resourceObject.getResourceObjectIdentifier().json() );
            return this;
        }

        public Builder resources( ResourceObjects resourceObjects )
        {
            for ( ResourceObject resource : resourceObjects.getResources() )
            {
                builder.add( resource.getResourceObjectIdentifier().json() );
            }
            return this;
        }

        public ResourceObjectIdentifiers build()
        {
            return new ResourceObjectIdentifiers( builder.build() );
        }
    }

    public ResourceObjectIdentifiers( JsonArray json )
    {
        super( json );
    }

    public List<ResourceObjectIdentifier> getResources()
    {
        return array().getValuesAs( ResourceObjectIdentifier::new );
    }
}
