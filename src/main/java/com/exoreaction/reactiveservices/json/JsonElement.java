/*
 *  Copyright (C) 2018 Real Vision Group SEZC
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.exoreaction.reactiveservices.json;

import jakarta.json.*;

import java.io.StringWriter;
import java.util.Optional;

/**
 * @author rickardoberg
 * @since 07/08/2017
 */
public interface JsonElement
{
    JsonStructure json();

    default JsonObject object()
    {
        JsonValue json = json();
        if ( json instanceof JsonObject)
            return (JsonObject) json;
        else
            return null;
    }

    default JsonArray array()
    {
        JsonValue json = json();
        if (json instanceof JsonArray )
            return (JsonArray) json();
        else
            return null;
    }

    default String getString( String name )
    {
        return object().getString( name );
    }

    default Optional<String> getOptionalString( String name )
    {
        return Optional.ofNullable( object().getString( name, null ) );
    }

    default int getInt( String name )
    {
        return object().getInt( name );
    }

    default Optional<Integer> getOptionalInt( String name )
    {
        JsonValue value = object().get( name );
        if ( value instanceof JsonNumber )
        {
            return Optional.of( ((JsonNumber) value).intValue() );
        }
        else
        { return Optional.empty(); }
    }

    default long getLong( String name )
    {
        return object().getJsonNumber( name ).longValueExact();
    }

    default Optional<Long> getOptionalLong( String name )
    {
        JsonValue value = object().get( name );
        if ( value instanceof JsonNumber )
        {
            return Optional.of( ((JsonNumber) value).longValueExact() );
        }
        else
        { return Optional.empty(); }
    }

    default boolean getBoolean( String name )
    {
        return object().getBoolean( name );
    }

    default Optional<Boolean> getOptionalBoolean( String name )
    {
        try
        {
            return Optional.of( object().getBoolean( name ) );
        }
        catch ( Exception e )
        {
            return Optional.empty();
        }
    }

    default <T extends Enum<T>> T getEnum( String name, Class<T> enumClass )
    {
        Optional<String> value = getOptionalString( name );

        if ( value.isPresent() )
        {
            try
            {
                return Enum.valueOf( enumClass, value.get() );
            }
            catch ( IllegalArgumentException e )
            {
                return null;
            }
        }
        else
        { return null; }
    }

    default <T extends Enum<T>> Optional<T> getOptionalEnum( String name, Class<T> enumClass )
    {
        Optional<String> value = getOptionalString( name );

        if ( value.isPresent() )
        {
            try
            {
                return Optional.of( Enum.valueOf( enumClass, value.get() ) );
            }
            catch ( IllegalArgumentException e )
            {
                return null;
            }
        }
        else
        { return Optional.empty(); }
    }

    default String toJsonString()
    {
        StringWriter out = new StringWriter();
        Json.createWriter( out ).write( json() );
        return out.toString();
    }

}
