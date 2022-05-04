package com.exoreaction.reactiveservices.jsonapi.model;

import com.exoreaction.reactiveservices.json.JsonElement;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;

import java.util.List;
import java.util.stream.Collector;

/**
 * @author rickardoberg
 * @since 28/11/2018
 */

public record ResourceObjects(JsonArray json)
    implements JsonElement
{
    public static Collector<ResourceObject,Builder,ResourceObjects> toResourceObjects()
    {
        return Collector.of( Builder::new, (builder, ro) -> {if (ro != null) builder.resource(ro);}, ( builder1, builder2 ) -> builder1, Builder::build );
    }

    public static class Builder
    {
        JsonArrayBuilder builder = Json.createArrayBuilder();

        public Builder resource( ResourceObject resourceObject )
        {
            builder.add( resourceObject.json() );
            return this;
        }

        public ResourceObjects build()
        {
            return new ResourceObjects( builder.build() );
        }
    }

    public List<ResourceObject> getResources()
    {
        return array().getValuesAs( ResourceObject::new );
    }

    public ResourceObjectIdentifiers getResourceObjectIdentifiers()
    {
        return new ResourceObjectIdentifiers.Builder().resources( this ).build();
    }
}
