package com.exoreaction.reactiveservices.jsonapi.model;

import com.exoreaction.reactiveservices.json.JsonElement;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * @author rickardoberg
 * @since 27/11/2018
 */

public record Included(JsonArray json)
    implements JsonElement, Consumer<Included.Builder>
{
    public static class Builder
    {
        private Set<String> included = new HashSet<>();
        private JsonArrayBuilder builder = Json.createArrayBuilder();

        public Builder include(ResourceObject resourceObject )
        {
            String resourceId = resourceObject.getId() + ":" + resourceObject.getType();
            if ( !included.contains( resourceId ) )
            {
                builder.add( resourceObject.json() );
                included.add( resourceId );
            }
            return this;
        }

        public ResourceObject add( ResourceObject resourceObject )
        {
            include( resourceObject );
            return resourceObject;
        }

        public ResourceObjects add( ResourceObjects resourceObjects )
        {
            include( resourceObjects );
            return resourceObjects;
        }

        public Builder include( ResourceObjects resourceObjects )
        {
            for ( ResourceObject resource : resourceObjects.getResources() )
            {
                include( resource );
            }
            return this;
        }

        public Builder with(Consumer<Builder> consumer)
        {
            consumer.accept(this);
            return this;
        }

        public Included build()
        {
            return new Included( builder.build() );
        }

        public boolean hasIncluded( String id, String type )
        {
            return included.contains( id + ":" + type );
        }

        public boolean hasIncluded( String id, Enum<?> type )
        {
            return included.contains( id + ":" + type );
        }
    }

    public List<ResourceObject> getIncluded()
    {
        return array().getValuesAs( ResourceObject::new );
    }

    public Optional<ResourceObject> findByResourceObjectIdentifier(ResourceObjectIdentifier roi)
    {
        return getIncluded().stream().filter(ro -> ro.getId().equals(roi.getId()) && ro.getType().equals(roi.getType())).findFirst();
    }

    @Override
    public void accept(Builder builder) {
        getIncluded().forEach(builder::include);
    }
}
