package com.exoreaction.reactiveservices.jsonapi.model;

import com.exoreaction.reactiveservices.json.JsonElement;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;

/**
 * @author rickardoberg
 * @since 23/01/2019
 */

public record Source(JsonObject json)
    implements JsonElement
{
    public static class Builder
    {
        JsonObjectBuilder builder = Json.createObjectBuilder();

        public Builder pointer( String value )
        {
            builder.add( "pointer", value );
            return this;
        }

        public Builder parameter( String value )
        {
            builder.add( "detail", value );
            return this;
        }

        public Source build()
        {
            return new Source( builder.build() );
        }
    }

    public String getPointer()
    {
        return getOptionalString( "pointer" ).orElse( null );
    }

    public String getParameter()
    {
        return getOptionalString( "parameter" ).orElse( null );
    }
}
