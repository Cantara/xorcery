package com.exoreaction.reactiveservices.jsonapi;

import com.exoreaction.reactiveservices.json.AbstractJsonElement;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonStructure;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author rickardoberg
 * @since 27/11/2018
 */

public class Included
    extends AbstractJsonElement
{
    public static class Builder
    {
        private Set<String> included = new HashSet<>();
        private JsonArrayBuilder builder = Json.createArrayBuilder();

        public Builder include( ResourceObject resourceObject )
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

    public Included( JsonStructure json )
    {
        super( json );
    }

    @Override
    public JsonArray array()
    {
        return super.array();
    }

    public List<ResourceObject> getIncluded()
    {
        return array().getValuesAs( ResourceObject::new );
    }
}
