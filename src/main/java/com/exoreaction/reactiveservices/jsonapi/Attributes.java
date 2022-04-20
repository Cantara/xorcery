package com.exoreaction.reactiveservices.jsonapi;

import com.exoreaction.reactiveservices.json.AbstractJsonElement;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonStructure;
import jakarta.json.JsonValue;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Period;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * @author rickardoberg
 * @since 27/11/2018
 */

public class Attributes
    extends AbstractJsonElement
{
    public static class Builder
    {
        JsonObjectBuilder builder = Json.createObjectBuilder();

        public Builder attribute( String name, Object value )
        {
            builder.add( name, getValue( value ) );
            return this;
        }

        public Builder attribute( String name, JsonValue value )
        {
            builder.add( name, value );
            return this;
        }

/*
        public Builder attributes( Map<String,Object> attributes, Predicate<String> fieldFilter )
        {
            attributes.forEach( ( key, value ) ->
            {
                if ( !fieldFilter.test( key ) )
                {
                    return;
                }

                builder.add( key, getValue( value ) );
            } );
            return this;
        }
*/

        public Builder with( Consumer<Builder> consumer )
        {
            consumer.accept( this );
            return this;
        }

        private JsonValue getValue( Object value )
        {
            if ( value == null )
            {
                return JsonValue.NULL;
            }
            else
            {
                if ( value instanceof String )
                {
                    return Json.createValue( (String) value );
                }
                else if ( value instanceof Boolean )
                {
                    return ((Boolean) value) ? JsonValue.TRUE : JsonValue.FALSE;
                }
                else if ( value instanceof Long )
                {
                    return Json.createValue( (Long) value );
                }
                else if ( value instanceof Integer )
                {
                    return Json.createValue( (Integer) value );
                }
                else if ( value instanceof BigDecimal )
                {
                    return Json.createValue( (BigDecimal) value );
                }
                else if ( value instanceof Double )
                {
                    return Json.createValue( new BigDecimal( (Double) value ).setScale( 2, RoundingMode.HALF_EVEN ) );
                }
                else if ( Double.TYPE.isInstance( value ) )
                {
                    return Json.createValue( new BigDecimal( (Double) value ).setScale( 2, RoundingMode.HALF_EVEN ) );
                }
                else if ( value instanceof Float )
                {
                    return Json.createValue( (Float) value );
                }
                else if ( value instanceof Enum )
                {
                    return Json.createValue( ((Enum) value).name() );
                }
                else if ( value instanceof Map )
                {
                    Set<Map.Entry<Object,Object>> set = ((Map<Object,Object>) value).entrySet();
                    JsonObjectBuilder map = Json.createObjectBuilder();
                    for ( Map.Entry<Object,Object> entry : set )
                    {
                        map.add( entry.getKey().toString(), getValue( entry.getValue() ) );
                    }
                    return map.build();
                }
                else if ( value instanceof Period )
                {
                    return Json.createValue( value.toString() );
                }
                else if ( value.getClass().isArray() )
                {
                    JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();

                    Object[] array = (Object[]) value;
                    for ( Object instance : array )
                    {
                        arrayBuilder.add( getValue( instance ) );
                    }
                    return arrayBuilder.build();
                }
                else if ( value instanceof Collection )
                {
                    JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
                    Collection collection = (Collection) value;
                    for ( Object instance : collection )
                    {
                        arrayBuilder.add( getValue( instance ) );
                    }
                    return arrayBuilder.build();
                }
                else
                {
                    throw new IllegalArgumentException( "Not a valid value type:" + value.getClass() );
                }
            }
        }

        public Attributes build()
        {
            return new Attributes( builder.build() );
        }
    }

    public Attributes( JsonStructure json )
    {
        super( json );
    }

    public JsonObject getAttributes()
    {
        return json().asJsonObject();
    }

    public Optional<JsonValue> getAttribute( String name )
    {
        return Optional.ofNullable( object().get( name ) );
    }
}
