package com.exoreaction.reactiveservices.jsonapi;

import com.exoreaction.reactiveservices.json.AbstractJsonElement;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonStructure;
import jakarta.json.JsonValue;
import jakarta.ws.rs.core.MediaType;

import java.util.Optional;

/**
 * @author rickardoberg
 * @since 27/11/2018
 */

public class ResourceDocument
    extends AbstractJsonElement
{
    /**
     * A {@code String} constant representing {@value #APPLICATION_JSON_API} media type.
     */
    public final static String APPLICATION_JSON_API = "application/vnd.api+json";

    /**
     * A {@link MediaType} constant representing {@value #APPLICATION_JSON_API} media type.
     */
    public final static MediaType APPLICATION_JSON_API_TYPE =
        new MediaType( "application", "vnd.api+json" );

    public static class Builder
    {
        JsonObjectBuilder builder = Json.createObjectBuilder();

        public Builder data( ResourceObject value )
        {
            if ( value == null )
            {
                builder.add( "data", JsonValue.NULL );
            }
            else
            {
                builder.add( "data", value.json() );
            }
            return this;
        }

        public Builder data( ResourceObjects value )
        {
            builder.add( "data", value.json() );
            return this;
        }

        public Builder errors( Errors value )
        {
            builder.add( "errors", value.json() );
            return this;
        }

        public Builder meta( Meta value )
        {
            if ( !value.object().isEmpty() )
            {
                builder.add( "meta", value.json() );
            }
            return this;
        }

        public Builder jsonapi( JsonApi value )
        {
            if ( !value.object().isEmpty() )
            {
                builder.add( "jsonapi", value.json() );
            }
            return this;
        }

        public Builder links( Links value )
        {
            if ( !value.object().isEmpty() )
            {
                builder.add( "links", value.json() );
            }
            return this;
        }

        public Builder included( Included value )
        {
            if ( !value.array().isEmpty() )
            {
                builder.add( "included", value.json() );
            }
            return this;
        }

        public ResourceDocument build()
        {
            return new ResourceDocument( builder.build() );
        }

    }

    public static Builder from( ResourceDocument resourceDocument )
    {
        Builder b = new Builder();
        b.builder = Json.createObjectBuilder(resourceDocument.object());
        return b;
    }

    public ResourceDocument( JsonStructure json )
    {
        super( json );
    }

    public boolean isCollection()
    {
        return object().get( "data" ) instanceof JsonArray;
    }

    public Optional<ResourceObject> getResource()
    {
        JsonValue data = object().get( "data" );
        if ( data == null || data == JsonValue.NULL || data instanceof JsonArray )
        {return Optional.empty();}

        return Optional.of( new ResourceObject( (JsonObject) data ) );
    }


    public Optional<ResourceObjects> getResources()
    {
        JsonValue data = object().get( "data" );
        if ( data == null || data instanceof JsonObject )
        {return Optional.empty();}

        return Optional.of( new ResourceObjects( data.asJsonArray() ) );
    }

    public Errors getErrors()
    {
        return new Errors(
            Optional.ofNullable( object().getJsonArray( "errors" ) ).orElse( JsonValue.EMPTY_JSON_ARRAY ) );
    }

    public Meta getMeta()
    {
        return new Meta(
            Optional.ofNullable( object().getJsonObject( "meta" ) ).orElse( JsonValue.EMPTY_JSON_OBJECT ) );
    }

    public JsonApi getJsonapi()
    {
        return new JsonApi(
            Optional.ofNullable( object().getJsonObject( "jsonapi" ) ).orElse( JsonValue.EMPTY_JSON_OBJECT ) );
    }

    public Links getLinks()
    {
        return new Links(
            Optional.ofNullable( object().getJsonObject( "links" ) ).orElse( JsonValue.EMPTY_JSON_OBJECT ) );
    }

    public Included getIncluded()
    {
        return new Included(
            Optional.ofNullable( object().getJsonArray( "included" ) ).orElse( JsonValue.EMPTY_JSON_ARRAY ) );
    }
}
