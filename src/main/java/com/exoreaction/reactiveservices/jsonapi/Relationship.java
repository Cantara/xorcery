package com.exoreaction.reactiveservices.jsonapi;

import com.exoreaction.reactiveservices.json.AbstractJsonElement;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonStructure;
import jakarta.json.JsonValue;

import java.net.URI;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * @author rickardoberg
 * @since 01/12/2018
 */

public class Relationship
    extends AbstractJsonElement
{
    public static class Builder
    {
        JsonObjectBuilder builder = Json.createObjectBuilder();

        public Builder links( Links value )
        {
            builder.add( "links", value.json() );
            return this;
        }

        public Builder link( String rel, URI value )
        {
            return links( new Links.Builder().link( rel, value ).build() );
        }

        public Builder link( String rel, String value )
        {
            return links( new Links.Builder().link( rel, value ).build() );
        }

        public Builder meta( Meta value )
        {
            if (!value.getMeta().isEmpty())
            {
                builder.add( "meta", value.json() );
            }
            return this;
        }

        public Builder resourceIdentifier( ResourceObjectIdentifier value )
        {
            builder.add( "data", value == null ? JsonValue.NULL : value.json() );
            return this;
        }

        public Builder resourceIdentifiers( ResourceObjectIdentifiers value )
        {
            builder.add( "data", value.json() );
            return this;
        }

        public Builder resourceIdentifier( ResourceObject value )
        {
            builder.add( "data", value == null ? JsonValue.NULL : value.getResourceObjectIdentifier().json() );
            return this;
        }

        public Builder resourceIdentifiers( ResourceObjects value )
        {
            builder.add( "data", value.getResourceObjectIdentifiers().json() );
            return this;
        }

        public Builder with( Consumer<Builder> consumer )
        {
            consumer.accept( this );
            return this;
        }

        public Relationship build()
        {
            return new Relationship( builder.build() );
        }
    }

    public Relationship( JsonStructure json )
    {
        super( json );
    }

    public boolean isIncluded()
    {
        return object().containsKey( "data" );
    }

    public boolean isMany()
    {
        return object().get( "data" ) instanceof JsonArray;
    }

    @Override
    public JsonArray array()
    {
        return null;
    }

    public Meta getMeta()
    {
        return new Meta(
            Optional.ofNullable( object().getJsonObject( "meta" ) ).orElse( JsonValue.EMPTY_JSON_OBJECT ) );
    }

    public Links getLinks()
    {
        return new Links(
            Optional.ofNullable( object().getJsonObject( "links" ) ).orElse( JsonValue.EMPTY_JSON_OBJECT ) );
    }

    public Optional<ResourceObjectIdentifier> getResourceObjectIdentifier()
    {
        JsonValue data = object().get( "data" );
        if ( data == null || data == JsonValue.NULL || data instanceof JsonArray )
        {
            return Optional.empty();
        }

        return Optional.of( new ResourceObjectIdentifier( (JsonObject) data ) );
    }

    public Optional<ResourceObjectIdentifiers> getResourceObjectIdentifiers()
    {
        JsonValue data = object().getJsonArray( "data" );
        if ( data == null || data instanceof JsonObject )
        { return Optional.empty(); }

        return Optional.of( new ResourceObjectIdentifiers( data.asJsonArray() ) );
    }
}
