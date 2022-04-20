package com.exoreaction.reactiveservices.jsonapi;

import com.exoreaction.reactiveservices.json.AbstractJsonElement;
import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonStructure;

/**
 * @author rickardoberg
 * @since 23/01/2019
 */

public class Source
    extends AbstractJsonElement
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

    public Source( JsonStructure json )
    {
        super( json );
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
