package com.exoreaction.reactiveservices.jsonapi;

import com.exoreaction.reactiveservices.json.AbstractJsonElement;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonStructure;
import jakarta.json.JsonValue;

import java.util.function.Consumer;

/**
 * @author rickardoberg
 * @since 27/11/2018
 */

public class Meta
    extends AbstractJsonElement
{
    public static class Builder
    {
        JsonObjectBuilder json = Json.createObjectBuilder();

        public Builder meta(String name, JsonValue value)
        {
            json.add( name, value );
            return this;
        }

        public Builder meta(String name, long value)
        {
            json.add( name, value );
            return this;
        }

        public Builder with( Consumer<Builder> consumer)
        {
            consumer.accept( this );
            return this;
        }

        public Meta build()
        {
            return new Meta(json.build());
        }
    }

    public Meta( JsonStructure json )
    {
        super( json );
    }

    public JsonObject getMeta()
    {
        return json().asJsonObject();
    }
}
