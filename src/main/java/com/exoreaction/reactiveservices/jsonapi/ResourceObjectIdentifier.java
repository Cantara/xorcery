package com.exoreaction.reactiveservices.jsonapi;

import com.exoreaction.reactiveservices.json.AbstractJsonElement;

import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonStructure;

import static java.util.Optional.ofNullable;
import static jakarta.json.JsonValue.EMPTY_JSON_OBJECT;

/**
 * @author rickardoberg
 * @since 27/11/2018
 */

public class ResourceObjectIdentifier
    extends AbstractJsonElement
{
    public static class Builder
    {
        private JsonObjectBuilder builder;

        public Builder( String id, String type )
        {
            builder = Json.createObjectBuilder();
            builder.add( "id", id )
                   .add( "type", type );
        }

        public Builder( String id, Enum<?> type )
        {
            this(id, type.name());
        }

        public Builder meta( Meta meta )
        {
            builder.add( "meta", meta.json() );
            return this;
        }

        public ResourceObjectIdentifier build()
        {
            return new ResourceObjectIdentifier( builder.build() );
        }
    }

    public ResourceObjectIdentifier( JsonStructure json )
    {
        super( json );
    }

    public String getId()
    {
        return object().getString( "id" );
    }

    public String getType()
    {
        return object().getString( "type" );
    }

    public Meta getMeta()
    {
        return new Meta( ofNullable( object().getJsonObject( "meta" ) ).orElse( EMPTY_JSON_OBJECT ) );
    }
}
