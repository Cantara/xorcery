package com.exoreaction.reactiveservices.jsonapi;

import com.exoreaction.reactiveservices.json.AbstractJsonElement;
import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonStructure;
import jakarta.json.JsonValue;

import java.util.Optional;

/**
 * @author rickardoberg
 * @since 23/01/2019
 */

public class Error
    extends AbstractJsonElement
{
    public static class Builder
    {
        JsonObjectBuilder builder = Json.createObjectBuilder();

        public Builder status(int value)
        {
            builder.add( "status", Integer.toString( value ) );
            return this;
        }

        public Builder source(Source source)
        {
            builder.add( "source", source.object() );
            return this;
        }

        public Builder title(String value)
        {
            builder.add( "title", value );
            return this;
        }

        public Builder detail(String value)
        {
            builder.add( "detail", value );
            return this;
        }

        public Error build()
        {
            return new Error(builder.build());
        }
    }

    public Error( JsonStructure json )
    {
        super( json );
    }

    public String getStatus()
    {
        return getOptionalString( "status" ).orElse( null );
    }

    public Source getSource()
    {
        return new Source(
            Optional.ofNullable( object().getJsonObject( "source" ) ).orElse( JsonValue.EMPTY_JSON_OBJECT ) );
    }

    public String getTitle()
    {
        return getOptionalString( "title" ).orElse( null );
    }

    public String getDetail()
    {
        return getOptionalString( "detail" ).orElse( null );
    }
}
