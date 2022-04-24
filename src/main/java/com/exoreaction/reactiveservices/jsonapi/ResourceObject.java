package com.exoreaction.reactiveservices.jsonapi;

import com.exoreaction.reactiveservices.json.AbstractJsonElement;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonStructure;

import static java.util.Optional.ofNullable;
import static jakarta.json.JsonValue.EMPTY_JSON_OBJECT;

/**
 * @author rickardoberg
 * @since 27/11/2018
 */

public class ResourceObject
    extends AbstractJsonElement
{
    public static class Builder
    {
        private JsonObjectBuilder builder;

        public Builder( String type, String id )
        {
            builder = Json.createObjectBuilder();
            builder.add( "id", id )
                   .add( "type", type );
        }

        public Builder( Enum<?> type, String id )
        {
            this(id, type.name());
        }

        public Builder( String type )
        {
            builder = Json.createObjectBuilder();
            builder.add( "type", type );
        }

        public Builder attributes( Attributes attributes )
        {
            JsonObject object = attributes.object();
            if (!object.isEmpty())
                builder.add( "attributes", object );
            return this;
        }

        public Builder relationships( Relationships relationships )
        {
            JsonObject object = relationships.object();
            if (!object.isEmpty())
                builder.add( "relationships", object );
            return this;
        }

        public Builder links( Links.Builder links )
        {
            return links(links.build());
        }

        public Builder links( Links links )
        {
            builder.add( "links", links.json() );
            return this;
        }

        public Builder meta( Meta meta )
        {
            builder.add( "meta", meta.json() );
            return this;
        }

        public ResourceObject build()
        {
            return new ResourceObject( builder.build() );
        }
    }

    public ResourceObject( JsonStructure json )
    {
        super( json );
    }

    public String getId()
    {
        return object().getString( "id", null );
    }

    public String getType()
    {
        return object().getString( "type" );
    }

    public ResourceObjectIdentifier getResourceObjectIdentifier()
    {
        return new ResourceObjectIdentifier.Builder( getId(), getType() ).build();
    }

    public Attributes getAttributes()
    {
        return new Attributes( ofNullable( object().getJsonObject( "attributes" ) ).orElse( EMPTY_JSON_OBJECT ) );
    }

    public Relationships getRelationships()
    {
        return new Relationships( ofNullable( object().getJsonObject( "relationships" ) ).orElse( EMPTY_JSON_OBJECT ) );
    }

    public Links getLinks()
    {
        return new Links( ofNullable( object().getJsonObject( "links" ) ).orElse( EMPTY_JSON_OBJECT ) );
    }

    public Meta getMeta()
    {
        return new Meta( ofNullable( object().getJsonObject( "meta" ) ).orElse( EMPTY_JSON_OBJECT ) );
    }
}
