package com.exoreaction.reactiveservices.jsonapi.model;

import com.exoreaction.reactiveservices.json.JsonElement;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author rickardoberg
 * @since 27/11/2018
 */

public record Errors(JsonArray json)
    implements JsonElement
{
    public static class Builder
    {
        JsonArrayBuilder builder = Json.createArrayBuilder();

        public Builder error( Error error )
        {
            builder.add( error.json() );
            return this;
        }

        public Errors build()
        {
            return new Errors( builder.build() );
        }
    }

    public boolean hasErrors()
    {
        return !array().isEmpty();
    }

    public List<Error> errors()
    {
        return array().getValuesAs( Error::new );
    }

    public Map<String,Error> errorMap()
    {
        Map<String,Error> map = new HashMap<>();
        for ( Error error : errors() )
        {
            String pointer = error.getSource().getPointer();
            if ( pointer != null )
            {
                pointer = pointer.substring( pointer.lastIndexOf( '/' ) + 1 );
            }
            map.put( pointer, error );
        }
        return map;
    }

    public String error()
    {
        for ( Error error : errors() )
        {
            if (error.getSource().getPointer() == null)
                return error.getTitle();
        }

        return null;
    }
}
