package com.exoreaction.reactiveservices.jsonapi.model;

import com.exoreaction.reactiveservices.json.JsonElement;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonValue;

import java.util.List;

/**
 * @author rickardoberg
 * @since 28/11/2018
 */

public record ResourceObjectIdentifiers(JsonArray json)
    implements JsonElement
{
    public record Builder(JsonArrayBuilder builder)
    {
        public Builder() {
            this(Json.createArrayBuilder());
        }
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

        public Builder resources( ResourceObjectIdentifiers resourceObjectIdentifiers )
        {
            resourceObjectIdentifiers.getResources().forEach(this::resource);
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

    public List<ResourceObjectIdentifier> getResources()
    {
        return array().getValuesAs( ResourceObjectIdentifier::new );
    }

    public boolean contains(ResourceObjectIdentifier resourceObjectIdentifier) {
        return getResources().stream().anyMatch(ro -> ro.equals(resourceObjectIdentifier));
    }
    public boolean contains(ResourceObject resourceObject) {
        return contains(resourceObject.getResourceObjectIdentifier());
    }
}
